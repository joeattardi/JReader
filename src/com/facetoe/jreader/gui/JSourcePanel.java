package com.facetoe.jreader.gui;

import com.facetoe.jreader.ProfileManager;
import com.facetoe.jreader.java.JavaObject;
import com.facetoe.jreader.java.JavaSourceFile;
import com.facetoe.jreader.java.JavaSourceFileParser;
import com.facetoe.jreader.utilities.Utilities;
import japa.parser.ParseException;
import org.apache.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.jdesktop.swingx.JXCollapsiblePane;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.PatternSyntaxException;


/**
 * Displays source code with syntax highlighting and cold folding.
 */
public class JSourcePanel extends AbstractPanel {
    private final Logger log = Logger.getLogger(this.getClass());

    /**
     * The text area to display the source code.
     */
    private RSyntaxTextArea codeArea;


    /**
     * The scollpane that contains the editor
     */
    private RTextScrollPane codeScrollPane;

    /**
     * The object that contains all the definitions for this source file.
     */
    private JavaSourceFile javaSourceFile;

    /**
     * Where this file is located on the file system.
     */
    private final String sourceFilePath;

    /**
     * The top level object for this source file. It could be a class or interface,
     * and there can be any number of nested classes or interfaces within it.
     */
    private JavaObject enclosingObject;

    /**
     * Profile manager instance for this panel.
     */
    private final ProfileManager profileManager;

    /**
     * Listeners that will be notified of parsing progress and search errors.
     */
    private final ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();

    /**
     * This source files name.
     */
    private String fileName;

    /**
     * Whether or not we are already waiting for the status information to be cleared.
     */
    private boolean waitingOnLabelReset = false;

    /**
     * SourceTree for this panel.
     */
    private SourceTree tree;

    /**
     * ScrollPane for this SourceTree.
     */
    private JScrollPane treeScrollPane;

    /**
     * JPanel that holds the SourceTree
     */
    private JPanel treePanel;

    /**
     * JXCollapible pane for displaying and hiding the SourceTree
     */
    private JXCollapsiblePane collapsiblePane;

    /**
     * Search panel for the SourceTree
     */
    private JPanel searchPanel;

    /**
     * AutoCompletion field for the SourceTree.
     */
    private AutoCompleteTextField searchField;
    private JButton btnSearch;

    /**
     * Creates a new instance of JSourcePanel and displays the contents of filePath.
     *
     * @param filePath of the file containing the code to be displayed.
     */
    public JSourcePanel(String filePath, ActionListener listener) {
        addActionListener(listener);
        profileManager = ProfileManager.getInstance();
        this.sourceFilePath = filePath;
        fileName = new File(sourceFilePath).getName();


        /* Set up our text area. */
        codeArea = new RSyntaxTextArea();
        codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        codeArea.setCodeFoldingEnabled(true);
        codeArea.setAntiAliasingEnabled(true);
        codeArea.setEditable(false);

        //TODO Figure out a way for the user to set themes.
        /* Read in the theme for this text area. */
        fireEvent(new ActionEvent(this, 0, "Loading file: " + fileName));
        try {
            InputStream in = getClass().getResourceAsStream("/com/facetoe/jreader/resources/themes/ideaTheme.xml");
            Theme theme = Theme.load(in);
            theme.apply(codeArea);
        } catch ( IOException e ) {
            log.error(e.toString(), e);
        } catch ( Exception ex ) {
            System.err.println("Something happened: " + ex.toString());
        }

        codeScrollPane = new RTextScrollPane(codeArea);
        codeScrollPane.setFoldIndicatorEnabled(true);
        setLayout(new BorderLayout());
        add(codeScrollPane);

        try {
            String code = Utilities.readFile(Paths.get(sourceFilePath).toString(), StandardCharsets.UTF_8);
            codeArea.setText(code);
        } catch ( IOException e ) {
            log.error(e.getMessage(), e);
        }

        /* Parse the source file and extract all the definitions. */
        parseSourceFile();

        if ( javaSourceFile != null ) {
            enclosingObject = javaSourceFile.getEnclosingClass();
        } else {
            log.error("Source file was null: " + filePath);
        }
        initTreeView();
    }

    private void initTreeView() {
        tree = new SourceTree(javaSourceFile);
        /* Save a click by showing the contents of the class on load. */
        tree.expandRow(0);
        tree.addTreeSelectionListener(new SourceTreeSelectionListener(tree, this));
        treeScrollPane = new JScrollPane(tree);
        treeScrollPane.setPreferredSize(new Dimension(300, 200));

        collapsiblePane = new JXCollapsiblePane();

        searchPanel = new JPanel(new BorderLayout());
        searchField = new AutoCompleteTextField();
        searchField.addWordsToTrie(javaSourceFile.getAllDeclarations());
        btnSearch = new JButton("Filter");

        btnSearch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleTreeSearcg();
            }
        });
        searchField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleTreeSearcg();
            }
        });
        String keyStrokeAndKey = "control T";
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyStrokeAndKey);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, keyStrokeAndKey);
        getActionMap().put(keyStrokeAndKey, collapsiblePane.getActionMap().get("toggle"));

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(btnSearch, BorderLayout.EAST);

        treePanel = new JPanel(new BorderLayout());
        treePanel.add(treeScrollPane, BorderLayout.CENTER);
        treePanel.add(searchPanel, BorderLayout.NORTH);


        collapsiblePane.setLayout(new BorderLayout());
        collapsiblePane.setDirection(JXCollapsiblePane.Direction.RIGHT);
        collapsiblePane.add("Center", treePanel);
        collapsiblePane.setCollapsed(true);

        setLayout(new BorderLayout());
        // Put the tree on the left
        add("West", collapsiblePane);

        // And the pane on the right.
        add("Center", codeScrollPane);

        // Show/hide the "Controls"
        JButton toggle = new JButton(collapsiblePane.getActionMap().get("toggle"));

        add("South", toggle);
    }

    /**
     * Highlights the parent class or interface for this file.
     * This needs to be called from JReader or the panel won't scroll
     * to the highlighted text.
     */
    public void highlightEnclosingObject() {
        highlightDeclaration(enclosingObject.getBeginLine(),
                enclosingObject.getEndLine(),
                enclosingObject.beginColumn);
    }

    /**
     * Parse the source file and provide feedback to any listeners.
     */
    private void parseSourceFile() {
        long startTime = System.nanoTime();
        fireEvent(new ActionEvent(this, 0, "Parsing: " + fileName));
        try {
            javaSourceFile = JavaSourceFileParser.parse(new FileInputStream(sourceFilePath));
        } catch ( ParseException e ) {
            log.error(e.toString(), e);
        } catch ( IOException e ) {
            log.error(e.toString(), e);
        } finally {
            long elapsedTime = System.nanoTime() - startTime;
            fireEvent(new ActionEvent(this, 0, String.format(
                    "Parsed %s in %.2f %s", fileName, ( double ) elapsedTime / 1000000000, "seconds")));
        }
    }

    @Override
    public ArrayList<String> getAutoCompleteWords() {
        return javaSourceFile.getAllDeclarations();
    }

    /**
     * This method first attempts to highlight the declaration. If nothing is found for the key,
     * it then attmepts to search for it.
     * @param key The declaration that we want to highlight.
     */
    @Override
    public void handleAutoComplete(String key) {
        JavaObject obj = javaSourceFile.getObject(key);

         if ( obj != null ) {
            highlightDeclaration(obj.getBeginLine(), obj.getEndLine(),
                    obj.beginColumn);
        } else {
            findString(key, profileManager.getSearchContext());
        }
    }

    private void handleTreeSearcg() {
        String text = searchField.getText();
        if(text.isEmpty()) {
            return;
        }

        DefaultMutableTreeNode root = (DefaultMutableTreeNode)tree.getModel().getRoot();
        for(Enumeration e = root.breadthFirstEnumeration() ; e.hasMoreElements() ;) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
            if(node.toString().equals(text)) {
                TreePath treePath = new TreePath(node.getPath());
                tree.setSelectionPath(treePath);
                tree.scrollPathToVisible(treePath);
                findString(text, profileManager.getSearchContext());
                return;
            }
        }
        System.out.println("No!");
    }

    /** This method attempts to find a string or regexp in the source file.
     *  It first searches from the current position to the end of the file,
     *  if that doesn't succeed it searches from the end of the file to the
     *  beginning.
     *
     *  It fires an ActionEvent if nothing is found or if there is a Regexp error.
     * @param text    to search for
     * @param context The search context for this search.
     */
    private void findString(String text, SearchContext context) {
        boolean found = false;
        int caretPos = codeArea.getCaretPosition();
        context.setSearchFor(text);
        context.setSearchForward(true);

        try {
            found = SearchEngine.find(codeArea, context);
            if ( !found ) {
                codeArea.setCaretPosition(0);
                found = SearchEngine.find(codeArea, context);
                if ( !found ) {
                /* If we didn't find anything, reset the caret position or it ends up
                 * jumping to the end or beginning of the file.. */
                    codeArea.setCaretPosition(caretPos);
                    fireEvent(new ActionEvent(this, 0, "Nothing found for: " + "\"" + text + "\""));
                }
            }
        } catch ( PatternSyntaxException ex ) {
            fireEvent(new ActionEvent(this, 0, "Regex Error: " + ex.getMessage()));
        }

        /* Reset the status label if we were successful. This is in a seperate thread for two reasons,
         * First, when the initial call succeeds in highlightEnclosingObject() the text would otherwise be deleted.
         * Doing it this way lets it hang around for a bit. Second, I think it looks better this way when a search
         * succeeds after an error.*/
         if ( found && !waitingOnLabelReset ) {
            SwingWorker worker = new SwingWorker() {
                @Override
                protected Object doInBackground() throws Exception {
                    waitingOnLabelReset = true;
                    Thread.sleep(5000);
                    fireEvent(new ActionEvent(this, 0, ""));
                    waitingOnLabelReset = false;
                    return null;
                }
            };
            worker.execute();

        }
    }

    /**
     * Highlights and scrolls to a method, constructor, field or enum declaration.
     * This method is necessary because really long method or function declarations are
     * formatted in the source like: <code>method(ReallyLongClassTypeThing superDescriptiveNameForYou,
     * AnotherAmazingLongObject incredibleyLongJavaName)</code>
     * <p/>
     * Attempts to search for these will fail because of the unexpected newline and spaces.
     * ,
     *
     * @param beginLine Where this selection starts.
     * @param endLine   The end of the entire block if it's a method or constructor.
     * @param beginCol  The column in which this selection begins.
     */
    void highlightDeclaration(int beginLine, int endLine, int beginCol) {
        try {
            int start = codeArea.getLineStartOffset(beginLine - 1);
            int end = codeArea.getLineEndOffset(endLine - 1);

            String body = codeArea.getText();

            //  If you don't do -1 it chops off the first character.
            start += beginCol - 1;
            String selectText = "";

            // Loop through until we hit a semicolon or opening bracket appending to selectText.
            for ( int i = start; i < end; i++ ) {
                if ( body.charAt(i) == '{' || body.charAt(i) == ';' ) {
                    break;
                }
                selectText += body.charAt(i);
            }

            // Now search for the text and it should succeed.
            findString(selectText, new SearchContext(selectText));

        } catch ( BadLocationException ex ) {
            log.error("Bad location in highlightDeclaration:", ex);
        }
    }

    /**
     * Add an action listener.
     * @param listener The listener to add.
     */
    void addActionListener(ActionListener listener) {
        listeners.add(listener);
    }

    /**
     * Notify listeners of progress.
     * @param event The progress information.
     */
    private void fireEvent(ActionEvent event) {
        for ( ActionListener listener : listeners ) {
            System.out.println("Fireing Event: " + event.getActionCommand());
            listener.actionPerformed(event);
        }
    }

    public JavaSourceFile getJavaSourceFile() {
        return javaSourceFile;
    }
}
