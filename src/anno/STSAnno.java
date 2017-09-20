package anno;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * @author Vuk Batanović
 * <br>
 * https://github.com/vukbatanovic/STSAnno
 */
public class STSAnno extends JFrame {
	
	// Interface strings
	private String txtFileExtensionString = "TXT file";
	private String windowTitleString = "Semantic textual similarity annotation";
	private String text1LabelString = "Text 1:";
	private String text2LabelString = "Text 2:";
	private String lineLabelString1 = "Line ";
	private String lineLabelString2 = " of ";
	private String scoredString = "Scored: ";
	private String unscoredString = "Unscored: ";
	private String skippedString = "Skipped: ";
	private String assignScoreButtonString = "Assign score";
	private String eraseScoreButtonString = "Erase score";
	private String saveButtonString = "Save data to file";
	private String jumpToNextPairCheckboxString = "Automatically jump to the next pair";
	private String emptyFileMessageString = "An empty file was chosen!";
	private String dataSavedMessageString = "Data saved!";
	
	// Constants
    private static final String [] scoreStrings = {"?","0","1","2","3","4","5"};
    private static final String SKIP_SIGN = "?";
    private static final int UNSCORED = 0;
    private static final int SCORED = 1;
    private static final int SKIPPED = -1;
    
    // Program logic
    private File corpusFile;
    private int lineCnt = 0;
    private int currentLine = 1;
    private String [] textPairArray = new String [0];
    private Integer [] statusArray = new Integer [0];
    private int scoredCnt = 0;
    private int unscoredCnt = 0;
    private int skippedCnt = 0;
    private boolean jumpToNext = false;
    
	// GUI Dimensions
	private Dimension windowDimension = new Dimension(750, 570);
	private Dimension textAreaDimension = new Dimension(650, 100);
	private Dimension scrollPaneDimension = new Dimension(650, 200);
	
	// GUI Elements
    private JTextArea text1Area = new JTextArea ();
    private JTextArea text2Area = new JTextArea ();
    private JPanel upperPanel = new JPanel ();
    private JPanel middlePanel = new JPanel ();
    private JPanel lowerPanel = new JPanel();
    private JPanel bottomPanel = new JPanel();
    private JPanel infoPanel = new JPanel();
    private JPanel labelPanel = new JPanel();
    private JButton assignScoreButton = new JButton (assignScoreButtonString);
    private JButton eraseScoreButton = new JButton (eraseScoreButtonString);
    private JButton saveButton = new JButton(saveButtonString);
    private JComboBox<String> scoreChooser = new JComboBox<String>(STSAnno.scoreStrings);
    private JLabel info1Label = new JLabel ();
    private JLabel info2Label = new JLabel ();
    private JLabel statisticsLabel = new JLabel ();
    private JTextField lineField = new JTextField ();
    private DefaultListModel<String> scrollPaneListModel = new DefaultListModel<String>();
    private JList<String> scrollPaneList = new JList<String>(scrollPaneListModel);
    private JScrollPane scrollPane = new JScrollPane(scrollPaneList);
    private JCheckBox jumpToNextPairCheckbox = new JCheckBox (jumpToNextPairCheckboxString);

    /**
     * A custom ListCellRenderer that highlights text pairs in the scroll pane according to their annotation status:
     * - White background - an unscored pair
     * - Gray background - a scored pair
     * - Yellow background - a pair that has been skipped
     */
    private class STSListCellRenderer extends DefaultListCellRenderer {
    	private Integer [] statusArray;
    	
    	public STSListCellRenderer (Integer [] statusArray) {
    		super();
    		this.statusArray = statusArray;
    	}
    	
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (!isSelected) {
				Color background = Color.WHITE;
				if (statusArray[index] == STSAnno.SKIPPED)
					background = Color.YELLOW;
				else if (statusArray[index] == STSAnno.SCORED)
					background = Color.LIGHT_GRAY;
				c.setBackground(background);
			}
			return c;
		}
    }
    
    /**
     * Input file selection dialog - STS corpus should be in a TXT file
     * @return Path to the STS corpus file
     */
    private String selectInputFile () {
        JFileChooser fc = new JFileChooser ();
        FileNameExtensionFilter ff = new javax.swing.filechooser.FileNameExtensionFilter(txtFileExtensionString, "txt");
        fc.setFileFilter(ff);
        fc.setCurrentDirectory(Paths.get(".").toFile());
        int returnVal = fc.showOpenDialog(null);
        if (returnVal != JFileChooser.APPROVE_OPTION)
        	System.exit(0);
        String filePath = fc.getSelectedFile().getAbsolutePath();
        return filePath;
    }
    
    /**
     * Reads in the data from the given STS corpus file
     * @param filePath Path to the STS corpus file
     * @throws IOException
     */
    private void readCorpusFileData (String filePath) throws IOException {
    	corpusFile = new File (filePath);
        BufferedReader br = new BufferedReader (new InputStreamReader (new FileInputStream(corpusFile), "UTF-8")); 
        String line;
        ArrayList<String> textPairArrayList = new ArrayList<String>();
        ArrayList<Integer> statusArrayList = new ArrayList<Integer>();
        while ((line = br.readLine()) !=null) {
            lineCnt++;
            textPairArrayList.add(line);
            String [] parts = line.split("\t");
            // If the text pair has already been scored or skipped
            if (parts.length == 3) {
            	if (!parts[0].equals(STSAnno.SKIP_SIGN)) {
            		statusArrayList.add(STSAnno.SCORED);
            		scoredCnt++;
            	}
            	else {
            		statusArrayList.add(STSAnno.SKIPPED);
            		skippedCnt++;
            	}
            }
            // If the text pair has not been considered so far
            else {
            	statusArrayList.add(STSAnno.UNSCORED);
            	unscoredCnt++;
            }
            scrollPaneListModel.addElement(line.replaceAll("\t", " "));
        }
        textPairArray = textPairArrayList.toArray(textPairArray);
        statusArray = statusArrayList.toArray(statusArray);
        br.close();
        
        // If an empty STS corpus file was given as input, display a message and exit 
        if (textPairArrayList.isEmpty()) {
            JOptionPane.showMessageDialog(this, emptyFileMessageString, "", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
    }
    
    /**
     * GUI initialization method
     */
    private void initializeGUI () {
        // Annotation information settings
        info1Label.setHorizontalAlignment(JLabel.CENTER);
        info2Label.setHorizontalAlignment(JLabel.CENTER);
        statisticsLabel.setHorizontalAlignment(JLabel.CENTER);
        infoPanel.add(info1Label);
        infoPanel.add(lineField);
        infoPanel.add(info2Label);
        lineField.setEditable(true);
        labelPanel.setLayout(new GridLayout(2,1));
        labelPanel.add(infoPanel);
        labelPanel.add(statisticsLabel);
        
        // TextArea settings
        text1Area.setEditable(false);
        text2Area.setEditable(false);
        text1Area.setLineWrap(true);
        text2Area.setLineWrap(true);
        text1Area.setWrapStyleWord(true);
        text2Area.setWrapStyleWord(true);
        text1Area.setPreferredSize(textAreaDimension);
        text2Area.setPreferredSize(textAreaDimension);
        upperPanel.setLayout(new BorderLayout ());
        upperPanel.add("Center", text1Area);
        upperPanel.add("North", new JLabel (text1LabelString));
        middlePanel.setLayout(new BorderLayout());
        middlePanel.add("Center", text2Area);
        middlePanel.add("North", new JLabel (text2LabelString));
        JPanel textPanel = new JPanel (new FlowLayout());
        textPanel.add(upperPanel);
        textPanel.add(middlePanel);
        
        // Command buttons settings
        lowerPanel.setLayout(new FlowLayout());
        lowerPanel.add(scoreChooser);
        lowerPanel.add(assignScoreButton);
        lowerPanel.add(eraseScoreButton);
        lowerPanel.add(saveButton);
        lowerPanel.add(jumpToNextPairCheckbox);
        
        // ScrollPane settings
        scrollPaneList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scrollPaneList.setLayoutOrientation(JList.VERTICAL);
        scrollPaneList.setCellRenderer(new STSListCellRenderer (statusArray));
        scrollPane.setPreferredSize(scrollPaneDimension);
        bottomPanel.add(scrollPane);
        
    	// Main window settings
        JPanel textAndCommandPanel = new JPanel (new BorderLayout());
        textAndCommandPanel.add("Center", textPanel);
        textAndCommandPanel.add("South", lowerPanel);
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add("Center", textAndCommandPanel);
        mainPanel.add("South", bottomPanel);
        mainPanel.add("North", labelPanel);
        this.setContentPane(mainPanel);
        this.setSize(windowDimension);
        this.setTitle(windowTitleString);
        this.setVisible(true);
        this.setLocationRelativeTo(null);
        this.setResizable(false);
    }
    
    /**
     * Adds the listeners for the ScrollPane, the line input field, the checkbox, all the buttons, and the main window
     */
    private void addListeners() {
    	/**
    	 * Move the ScrollPane to the selected pair in the corpus and update the TextAreas and the info section
    	 */
    	scrollPaneList.addListSelectionListener(new ListSelectionListener() {
    		public void valueChanged(ListSelectionEvent arg0) {
				setPair(scrollPaneList.getSelectedIndex());
				scrollPaneList.ensureIndexIsVisible(scrollPaneList.getSelectedIndex());
			}});
    	
    	/**
    	 * Move the ScrollPane to the pair whose line number has been entered in the text field
    	 * Update the TextAreas and the info section
    	 */
      	lineField.addActionListener(new ActionListener () {
        	public void actionPerformed (ActionEvent e) {
        		try {
        			int lineInd = Integer.parseInt(lineField.getText());
        			if (lineInd > 0 && lineInd <= lineCnt)
                		// The setPair method is called implicitly here, via the ListSelectionListener valueChanged method
                		scrollPaneList.setSelectedIndex(lineInd-1);
        		}
        		catch (NumberFormatException ex) {}
        	}});
    	
      	/**
      	 * Assign the selected score from the drop-down list to the current text pair
      	 */
    	assignScoreButton.addActionListener(new ActionListener () {
            public void actionPerformed (ActionEvent e) {
                writeScore((String)scoreChooser.getSelectedItem());
            }});

    	/**
    	 * Erase the similarity score assigned to the current text pair
    	 */
        eraseScoreButton.addActionListener(new ActionListener () {
            public void actionPerformed (ActionEvent e) {
                writeScore(null);
            }});
          
        /**
         * Save the (partially) annotated corpus in its current state to the starting corpus file, which is overwritten
         */
        saveButton.addActionListener(new ActionListener () {
            public void actionPerformed (ActionEvent e) {
                saveToFile();
                JOptionPane.showMessageDialog(null, dataSavedMessageString, "", JOptionPane.INFORMATION_MESSAGE);
            }});
          
        /**
         * If this checkbox is selected the program will automatically jump to the first unscored/skipped text pair after the current pair is annotated
         */
        jumpToNextPairCheckbox.addActionListener(new ActionListener () {
          	public void actionPerformed (ActionEvent e) {
          		if (jumpToNextPairCheckbox.isSelected())
          			jumpToNext = true;
          		else
          			jumpToNext = false;
          	}});

        /**
         * Saves the (partially) annotated corpus in its current state to the starting corpus file, which is overwritten, and closes the main window
         */
        this.addWindowListener(new WindowAdapter () {
            public void windowClosing (WindowEvent e) {
                finish ();
            }});
    }
    
    /**
     * Update the scored, unscored and skipped text pairs count display
     */
    private void updateStatisticsLabel () {
        statisticsLabel.setText(scoredString + scoredCnt + "          " + unscoredString + unscoredCnt + "          " + skippedString + skippedCnt);
    }
    
    /**
     * Update the current line info display and the annotation statistics info display
     */
    private void updateInfoSection() {
        info1Label.setText(lineLabelString1);
        lineField.setText(Integer.toString(currentLine+1));
        info2Label.setText(lineLabelString2 + lineCnt);
        updateStatisticsLabel();
    }

    /**
     * Finds the first unscored text pair in the STS corpus and selects it.
     * If there are no unscored pairs, the first skipped pair is selected.
     * If all the pairs are annotated, the first pair is selected.
     */
    private void findNextPair () {  
    	updateInfoSection();
        for (int i=0; i<statusArray.length; i++) {
        	if (statusArray[i] == STSAnno.UNSCORED) {
        		// The setPair method is called implicitly here, via the ListSelectionListener valueChanged method
        		scrollPaneList.setSelectedIndex(i);
        		return;
        	}
        }
        for (int i=0; i<statusArray.length; i++) {
        	if (statusArray[i] == STSAnno.SKIPPED) {
        		// The setPair method is called implicitly here, via the ListSelectionListener valueChanged method
        		scrollPaneList.setSelectedIndex(i);
        		return;
        	}
        }
		// The setPair method is called implicitly here, via the ListSelectionListener valueChanged method
        scrollPaneList.setSelectedIndex(0);
    }
    
    /**
     * Display the text pair from the selected line
     * @param selectedLine The ordinal number of the selected line from the corpus file
     */
    private void setPair(int selectedLine) {
    	currentLine = selectedLine;
        String [] parts = textPairArray[currentLine].split("\t");
        if (parts.length == 2) {
            text1Area.setText(parts[0]);
            text2Area.setText(parts[1]);
            scoreChooser.setSelectedIndex(0);
        }
        else {
            text1Area.setText(parts[1]);
            text2Area.setText(parts[2]);
            if (parts[0].startsWith(STSAnno.SKIP_SIGN))
            	scoreChooser.setSelectedIndex(0);
            else {
            	if (parts[0].length() > 1)
            		parts[0] = parts[0].substring(1);
            	scoreChooser.setSelectedIndex(1+Integer.parseInt(parts[0]));
            }
        }
        updateInfoSection();
    }
    
    /**
     * Assigns the given similarity score (or the skipped sign) to the current text pair
     * @param score The similarity score to be assigned to the current text pair; if score = null the current score is erased
     */
    private void writeScore (String score) {
    	String selectedTextPair = textPairArray[scrollPaneList.getSelectedIndex()];
    	String output = "";
    	if (selectedTextPair.split("\t").length == 2) {
    		if (score != null) {
    			output = score + "\t" + selectedTextPair;
				unscoredCnt--;
    			scrollPaneListModel.setElementAt(score + " " + selectedTextPair, scrollPaneList.getSelectedIndex());	
    			if (score.equals(STSAnno.SKIP_SIGN)) {
    				statusArray[scrollPaneList.getSelectedIndex()] = STSAnno.SKIPPED;
    				skippedCnt++;
    			}
    			else {
    				statusArray[scrollPaneList.getSelectedIndex()] = STSAnno.SCORED;
    				scoredCnt++;
    			}
    		}
    	}
    	else {
    		String [] temp = selectedTextPair.split("\t");
    		if (score != null) {
    			output = score + "\t" + temp[1] + "\t" + temp[2];
    			scrollPaneListModel.setElementAt(score + " " + temp[1] + " " + temp[2], scrollPaneList.getSelectedIndex());
    			if (score.equals(STSAnno.SKIP_SIGN)) {
    				statusArray[scrollPaneList.getSelectedIndex()] = STSAnno.SKIPPED;
    				if (!temp[0].equals(STSAnno.SKIP_SIGN)) {
    					scoredCnt--;
    					skippedCnt++;
    				}
    			}
    			else {
    				statusArray[scrollPaneList.getSelectedIndex()] = STSAnno.SCORED;
    				if (temp[0].equals(STSAnno.SKIP_SIGN)) {
    					scoredCnt++;
    					skippedCnt--;
    				}
    			}
    		}
    		// If the given score is null any existing annotation for the current pair is erased
    		else {
    			if (temp[0].equals(STSAnno.SKIP_SIGN))
    				skippedCnt--;
    			else
    				scoredCnt--;
    			unscoredCnt++;
    			output = temp[1] + "\t" + temp[2];
    			scrollPaneListModel.setElementAt(temp[1] + " " + temp[2], scrollPaneList.getSelectedIndex());
    			statusArray[scrollPaneList.getSelectedIndex()] = STSAnno.UNSCORED;
    		}
    	}
		textPairArray[scrollPaneList.getSelectedIndex()] = output;
        if (jumpToNext)
        	findNextPair ();
        else
            updateStatisticsLabel();
    }
    
    /**
     * Save the (partially) annotated corpus in its current state to the starting corpus file, which is overwritten
     */
    private void saveToFile () {
        try {
        	corpusFile.delete();
            corpusFile.createNewFile();
            PrintWriter fwout = new PrintWriter (corpusFile, "UTF-8");
            for (String s: textPairArray)
                fwout.println(s);
            fwout.close();
        }
        catch (IOException e) {}
    }
    
    /**
     * Saves the (partially) annotated corpus in its current state to the starting corpus file, which is overwritten, and closes the main window
     */
    private void finish () {
    	saveToFile();
        this.dispose();
    }
    
    /**
     * Main class of the STS annotation program
     * @throws IOException
     */
    public STSAnno () throws IOException {
    	
    	String corpusFilePath = selectInputFile();
    	readCorpusFileData(corpusFilePath);
        initializeGUI();
        addListeners();
        findNextPair ();
    }
    
    public static void main (String [] args) {
        try {
       		new STSAnno ();
        } catch (IOException ex) {
            Logger.getLogger(STSAnno.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}