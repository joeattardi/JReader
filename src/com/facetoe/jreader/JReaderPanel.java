package com.facetoe.jreader;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import static javafx.concurrent.Worker.State.FAILED;

/**
 * Displays the Java documentation.
 */
public class JReaderPanel extends JPanel {
    private final Logger log = Logger.getLogger(this.getClass());

    private WebEngine engine;
    private WebView view;
    private JFXPanel jfxPanel;
    private JProgressBar progressBar;
    private CountDownLatch latch;
    private final ProfileManager profileManager = ProfileManager.getInstance();

    private String currentPage;
    private String initialURL;

    /**
     * Create a new JReaderPanel instance with the specified URL.
     *
     * @param url          to display when the panel is loaded.
     * @param jProgressBar reference to the main JReader progress bar so we can display progress.
     */
    public JReaderPanel(String url, JProgressBar jProgressBar, CountDownLatch latch) {
        this.latch = latch;
        init(url, jProgressBar);
    }

    /**
     * Create a new JReaderPanel instance. Sets the initial URL to the index of the Java documentation.
     *
     * @param jProgressBar reference to of the main JReader progress bar so we can display progress.
     */
    public JReaderPanel(JProgressBar jProgressBar, CountDownLatch latch) {
        this.latch = latch;
        init(profileManager.getDocDir() + File.separator + "overview-summary.html", jProgressBar);
    }

    /**
     * Initialize everything and load the url
     *
     * @param url          to display when the panel is loaded
     * @param jProgressBar reference to of the main JReader progress bar so we can display progress.
     */
    private void init(String url, JProgressBar jProgressBar) {
        initialURL = url;
        progressBar = jProgressBar;
        jfxPanel = new JFXPanel();
        setLayout(new BorderLayout());
        createScene();
        loadURL(initialURL);
        log.debug("Finished loading JReaderPanel");
    }

    /**
     * Set up the javafx panel and add listeners.
     */
    private void createScene() {

        /**
         * Always modify javafx data in a javafx thread.
         */
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                view = new WebView();
                view.setContextMenuEnabled(false);

                engine = view.getEngine();

                engine.getLoadWorker().workDoneProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, final Number newValue) {

                        /**
                         * Anytime you want to modify a swing component from a javafx component you need to
                         * do it in the swing thread.
                         */
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setValue(newValue.intValue());
                            }
                        });
                    }
                });

                engine.locationProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> ov, String oldValue, final String newValue) {
                        log.debug("New Path: " + newValue);
                        if ( !newValue.endsWith(".java") ) {
                            currentPage = newValue;
                        }
                    }
                });

                engine.getLoadWorker()
                        .exceptionProperty()
                        .addListener(new ChangeListener<Throwable>() {

                            public void changed(ObservableValue<? extends Throwable> o, Throwable old, final Throwable value) {
                                if ( engine.getLoadWorker().getState() == FAILED ) {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            JOptionPane.showMessageDialog(
                                                    null,
                                                    (value != null) ?
                                                            engine.getLocation() + "\n" + value.getMessage() :
                                                            engine.getLocation() + "\nUnexpected error.",
                                                    "Loading error...",
                                                    JOptionPane.ERROR_MESSAGE);
                                        }
                                    });
                                }
                            }
                        });

                jfxPanel.setScene(new Scene(view));
                add(jfxPanel);
                log.debug("Counted down latch");

                /* Release the latch on the Swing thread. */
                latch.countDown();
            }
        });
    }

    /**
     * Navigate to the next page.
     */
    public void next() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                getEngine().executeScript("history.forward()");
            }
        });
    }

    /**
     * Navigate to the previous page.
     */
    public void back() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                getEngine().executeScript("history.back()");
            }
        });
    }

    /**
     * Navigate to the home page
     */
    public void home() {
        File overviewFile = new File(profileManager.getDocDir() +  File.separator +  "overview-summary.html");
        if(overviewFile == null) {
            System.err.println("Failed to load overview file at: " + overviewFile.getAbsolutePath());
        } else {
            loadURL(overviewFile.getAbsolutePath());
        }
    }

    /**
     * Load a url. Note that because we are modifying javafx data from swing we need to
     * do it in the javafx thread.
     *
     * @param url to load.
     */
    public void loadURL(final String url) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {

                /* This check is necessary because passing a url that starts with file:// to Paths.get(url).toUri()
                 * results in a mutated path on Linux - although it seemed to work fine on Windows.
                 * */
                if ( url.startsWith("file://") ) {
                    engine.load(url);
                } else {
                    String path = url;
                    try {
                        path = Paths.get(url).toUri().toString();
                    } catch ( InvalidPathException ex ) {
                    }
                    engine.load(path);
                }

            }
        });
    }

    /**
     * Return the WebEngine associated with this instance.
     *
     * @return the WebEngine
     */
    public WebEngine getEngine() {
        return engine;
    }

    /**
     * Return the JFXpanel associated with this instance.
     *
     * @return the JFXpanel
     */
    public JFXPanel getJFXPanel() {
        return jfxPanel;
    }

    /**
     * Return the current page.
     *
     * @return the current page.
     */
    public String getCurrentPage() {
        return currentPage;
    }
}

