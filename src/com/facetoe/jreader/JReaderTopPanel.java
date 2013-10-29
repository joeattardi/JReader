package com.facetoe.jreader;

import javax.swing.*;
import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: facetoe
 * Date: 28/10/13
 * Time: 11:36 AM
 */
public class JReaderTopPanel extends JPanel {

    private JButton btnBack;
    private JButton btnNext;
    private JButton btnHome;
    private JButton btnSearch;
    private JButton btnSource;
    private final AutoCompleteTextField searchBar = new AutoCompleteTextField();

    private final JReader jReader;

    public JReaderTopPanel(JReader jReader) {
        this.jReader = jReader;
        initButtons();
        initTopPanel();
    }


    /**
     * Create the buttons and add icons.
     */
    private void initButtons() {
        btnBack = new JButton(Utilities.readIcon(this.getClass().getResourceAsStream
                ("/com/facetoe/jreader/resources/icons/arrow-left.png"), 20, 20));

        btnNext = new JButton(Utilities.readIcon(this.getClass().getResourceAsStream
                ("/com/facetoe/jreader/resources/icons/arrow-right.png"), 20, 20));

        btnHome = new JButton(Utilities.readIcon(this.getClass().getResourceAsStream
                ("/com/facetoe/jreader/resources/icons/home.png"), 20, 20));

        btnSource = new JButton(new ViewSourceAction(this.jReader));
        btnSource.setIcon(Utilities.readIcon(this.getClass().getResourceAsStream
                ("/com/facetoe/jreader/resources/icons/javaSource.png"), 20, 20));

        btnSearch = new JButton(Utilities.readIcon(this.getClass().getResourceAsStream
                ("/com/facetoe/jreader/resources/icons/search.png"), 20, 20));
    }

    /**
     * Put the whole panel together.
     */
    private void initTopPanel() {
        setLayout(new BorderLayout(5, 0));
        JPanel leftBar = new JPanel(new FlowLayout());
        JPanel rightBar = new JPanel(new FlowLayout());

        searchBar.setPreferredSize(new Dimension(500, 30));

        leftBar.add(searchBar);
        leftBar.add(btnSearch);
        ImageIcon loaderGif = Utilities.readAnimatedGif(
                getClass().getResource("/com/facetoe/jreader/resources/icons/ajax-loader.gif"), this);


        JLabel label = new JLabel();
        label.setIcon(loaderGif);
        loaderGif.setImageObserver(label);
        leftBar.add(label);


        rightBar.add(btnBack);
        rightBar.add(btnNext);
        rightBar.add(btnHome);
        rightBar.add(btnSource);

        add(leftBar, BorderLayout.WEST);
        add(rightBar, BorderLayout.EAST);
        setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
    }

    public JButton getBtnBack() {
        return btnBack;
    }

    public JButton getBtnNext() {
        return btnNext;
    }

    public JButton getBtnHome() {
        return btnHome;
    }

    public JButton getBtnSearch() {
        return btnSearch;
    }

    public JButton getBtnSource() {
        return btnSource;
    }

    public AutoCompleteTextField getSearchBar() {
        return searchBar;
    }
}