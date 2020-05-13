package info.ginj;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * UI transparency based on sample code by MadProgrammer at https://stackoverflow.com/questions/26205164/java-custom-shaped-frame-using-image
 */
public class MainWindow {

    public static final int WINDOW_WIDTH_PIXELS = 150;
    public static final int WINDOW_HEIGHT_PIXELS = 150;

    public static final int SCREEN_CORNER_DEAD_ZONE_Y_PIXELS = 100;
    public static final int SCREEN_CORNER_DEAD_ZONE_X_PIXELS = 100;

    public static final float OPACITY_HALF = 0.5f;
    public static final float OPACITY_FULL = 1.0f;

    // Button sizes
    public static final int LARGE_SIZE_PIXELS = 40;
    public static final int MEDIUM_SIZE_PIXELS = 30;
    public static final int SMALL_SIZE_PIXELS = 20;

    // Button distance from center of sun to center of button
    public static final int LARGE_RADIUS_PIXELS = 55;
    public static final int MEDIUM_RADIUS_PIXELS = 55;
    public static final int SMALL_RADIUS_PIXELS = 45;

    // Button index
    public static final int BTN_NONE = -1;
    public static final int BTN_CAPTURE = 0;
    public static final int BTN_HISTORY = 1;
    public static final int BTN_MORE = 2;

    // Button "states"
    public static final int LARGE = 0;
    public static final int MEDIUM = 1;
    public static final int SMALL = 2;

    // Precomputed constants
    private static final int OFFSET_X_LARGE = WINDOW_WIDTH_PIXELS / 2 - LARGE_SIZE_PIXELS / 2;
    private static final int OFFSET_X_MEDIUM = WINDOW_WIDTH_PIXELS / 2 - MEDIUM_SIZE_PIXELS / 2;
    private static final int OFFSET_X_SMALL = WINDOW_WIDTH_PIXELS / 2 - SMALL_SIZE_PIXELS / 2;
    private static final int OFFSET_Y_LARGE = WINDOW_HEIGHT_PIXELS / 2 - LARGE_SIZE_PIXELS / 2;
    private static final int OFFSET_Y_MEDIUM = WINDOW_HEIGHT_PIXELS / 2 - MEDIUM_SIZE_PIXELS / 2;
    private static final int OFFSET_Y_SMALL = WINDOW_HEIGHT_PIXELS / 2 - SMALL_SIZE_PIXELS / 2;
    private static final int SMALL_RADIUS_PIXELS_DIAG = (int) Math.round((SMALL_RADIUS_PIXELS * Math.sqrt(2)) / 2);
    private static final int MEDIUM_RADIUS_PIXELS_DIAG = (int) Math.round((MEDIUM_RADIUS_PIXELS * Math.sqrt(2)) / 2);
    private static final int LARGE_RADIUS_PIXELS_DIAG = (int) Math.round((LARGE_RADIUS_PIXELS * Math.sqrt(2)) / 2);


    // Caching
    private final Dimension screenSize;
    private final Image[][] buttonImg = new Image[3][3]; // 3 buttons x 3 sizes
    Point[][] deltasByPosAndSize = new Point[3][3]; // 3 buttons x 3 sizes
    private Color defaultPaneBackground;

    // Current state
    private boolean isWindowFocused = false;
    private boolean isDragging = false;
    private int highlightedButtonId = BTN_NONE;

    JWindow window;

    public MainWindow() {
        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        window = new JWindow();
        window.setBackground(new Color(0, 0, 0, 0));
        MainPane contentPane = new MainPane();
        window.setContentPane(contentPane);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        window.setOpacity(OPACITY_HALF);
        positionWindowOnStartup();
        addMouseBehaviour(contentPane);
        window.setAlwaysOnTop(true);
    }


    public class MainPane extends JPanel {
        private BufferedImage sunOnlyImg;
        private BufferedImage sunRaysImg;
        // Array of images for the buttons.

        public MainPane() {
            try {
                sunOnlyImg = ImageIO.read(getClass().getResource("img/sun-only.png"));
                sunRaysImg = ImageIO.read(getClass().getResource("img/sun-rays.png"));

                Image originalImg = ImageIO.read(getClass().getResource("img/capture.png"));
                buttonImg[BTN_CAPTURE][LARGE] = originalImg.getScaledInstance(LARGE_SIZE_PIXELS, LARGE_SIZE_PIXELS, Image.SCALE_DEFAULT);
                buttonImg[BTN_CAPTURE][MEDIUM] = originalImg.getScaledInstance(MEDIUM_SIZE_PIXELS, MEDIUM_SIZE_PIXELS, Image.SCALE_DEFAULT);
                buttonImg[BTN_CAPTURE][SMALL] = originalImg.getScaledInstance(SMALL_SIZE_PIXELS, SMALL_SIZE_PIXELS, Image.SCALE_DEFAULT);

                originalImg = ImageIO.read(getClass().getResource("img/history.png"));
                buttonImg[BTN_HISTORY][LARGE] = originalImg.getScaledInstance(LARGE_SIZE_PIXELS, LARGE_SIZE_PIXELS, Image.SCALE_DEFAULT);
                buttonImg[BTN_HISTORY][MEDIUM] = originalImg.getScaledInstance(MEDIUM_SIZE_PIXELS, MEDIUM_SIZE_PIXELS, Image.SCALE_DEFAULT);
                buttonImg[BTN_HISTORY][SMALL] = originalImg.getScaledInstance(SMALL_SIZE_PIXELS, SMALL_SIZE_PIXELS, Image.SCALE_DEFAULT);

                originalImg = ImageIO.read(getClass().getResource("img/more.png"));
                buttonImg[BTN_MORE][LARGE] = originalImg.getScaledInstance(LARGE_SIZE_PIXELS, LARGE_SIZE_PIXELS, Image.SCALE_DEFAULT);
                buttonImg[BTN_MORE][MEDIUM] = originalImg.getScaledInstance(MEDIUM_SIZE_PIXELS, MEDIUM_SIZE_PIXELS, Image.SCALE_DEFAULT);
                buttonImg[BTN_MORE][SMALL] = originalImg.getScaledInstance(SMALL_SIZE_PIXELS, SMALL_SIZE_PIXELS, Image.SCALE_DEFAULT);

            }
            catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }

            // Store the background so it can be changed and restored later on
            defaultPaneBackground = getBackground();
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(sunOnlyImg.getWidth(), sunOnlyImg.getHeight());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            if (isWindowFocused) {
                // Image with rays
                g2d.drawImage(sunRaysImg, 0, 0, this);
                if (!isDragging) {
                    // Draw 3 action icons, with size and position depending on highlight state
                    for (int button = 0; button < 3; button++) {
                        if (highlightedButtonId == BTN_NONE) {
                            g2d.drawImage(buttonImg[button][MEDIUM], deltasByPosAndSize[button][MEDIUM].x, deltasByPosAndSize[button][MEDIUM].y, this);
                        }
                        else {
                            if (button == highlightedButtonId) {
                                g2d.drawImage(buttonImg[button][LARGE], deltasByPosAndSize[button][LARGE].x, deltasByPosAndSize[button][LARGE].y, this);
                            }
                            else {
                                g2d.drawImage(buttonImg[button][SMALL], deltasByPosAndSize[button][SMALL].x, deltasByPosAndSize[button][SMALL].y, this);
                            }
                        }
                    }
                }
            }
            else {
                // Image without rays
                g2d.drawImage(sunOnlyImg, 0, 0, this);
            }
            g2d.dispose();
        }
    }


    private void addMouseBehaviour(MainPane contentPane) {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            private int pX;
            private int pY;

            @Override
            public void mouseEntered(MouseEvent e) {
                changeFocus(contentPane, true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                changeFocus(contentPane, false);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // Get x,y and store them
                pX = e.getX();
                pY = e.getY();
                isDragging = true;
                window.repaint();
            }

            // Move window to border closest to center
            @Override
            public void mouseReleased(MouseEvent e) {
                window.setLocation(getClosestPointOnScreenBorder());
                isDragging = false;
                window.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                window.setLocation(window.getLocation().x + e.getX() - pX, window.getLocation().y + e.getY() - pY);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (!isDragging) {
                    int hoveredButtonId = getButtonIdAtLocation(e.getX(), e.getY());
                    if (hoveredButtonId != highlightedButtonId) {
                        highlightedButtonId = hoveredButtonId;
                        window.repaint();
                    }
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isDragging) {
                    int clickedButtonId = getButtonIdAtLocation(e.getX(), e.getY());
                    // ignore other clicks
                    switch (clickedButtonId) {
                        case BTN_CAPTURE -> onCapture();
                        case BTN_HISTORY -> onHistory();
                        case BTN_MORE -> onMore();
                    }
                    isWindowFocused = false;
                    window.repaint();
                }
            }

            // Find which button is being hovered, if any
            private int getButtonIdAtLocation(int x, int y) {
                int buttonId = BTN_NONE;
                for (int buttonIndex = 0; buttonIndex < 3; buttonIndex++) {
                    if (x >= deltasByPosAndSize[buttonIndex][LARGE].x
                            && x < deltasByPosAndSize[buttonIndex][LARGE].x + LARGE_SIZE_PIXELS
                            && y >= deltasByPosAndSize[buttonIndex][LARGE].y
                            && y < deltasByPosAndSize[buttonIndex][LARGE].y + LARGE_SIZE_PIXELS
                    ) {
                        buttonId = buttonIndex;
                        break;
                    }
                }
                return buttonId;
            }

        };

        window.addMouseListener(mouseAdapter);
        window.addMouseMotionListener(mouseAdapter);
    }

    private void changeFocus(MainPane contentPane, boolean focused) {
        isWindowFocused = focused;
        if (focused) {
            // Show the handle as opaque
            window.setOpacity(OPACITY_FULL);
            // And make the background of the window "visible", but filled with an almost transparent color
            // This has the effect of capturing mouse events on the full rectangular Window once it is focused,
            // which is necessary so that mouse doesn't "fall in the transparent holes" causing MouseExited events that
            // make the window "retract" to the handle-only view
            contentPane.setOpaque(true);
            contentPane.setBackground(new Color(0, 0, 0, 1)); // 1/255 opacity
        }
        else {
            // Show the handle as semi-transparent
            window.setOpacity(OPACITY_HALF);
            // And make the background of the window invisible, so that all mouse events and clicks "pass through"
            contentPane.setOpaque(false);
            contentPane.setBackground(defaultPaneBackground); // Strangely enough, setting it to a transparent color break things up
        }
    }


    private void positionWindowOnStartup() {
        int retrievedX = (int) (screenSize.getWidth() / 2);
        int retrievedY = 0;

        int x = retrievedX - window.getWidth() / 2;
        int y = retrievedY - window.getHeight() / 2;
        window.setLocation(x, y);

        computeButtonPositions(retrievedX, retrievedY);
    }

    private Point getClosestPointOnScreenBorder() {
        // Compute window center
        int centerX = window.getLocation().x + window.getWidth() / 2;
        int centerY = window.getLocation().y + window.getHeight() / 2;

        // Closest to left or right ?
        int distanceX;
        int targetX;
        if (centerX < screenSize.width - centerX) {
            distanceX = centerX;
            targetX = 0;
        }
        else {
            distanceX = screenSize.width - centerX;
            targetX = screenSize.width;
        }

        // Closest to top or bottom ?
        int distanceY;
        int targetY;
        if (centerY < screenSize.height - centerY) {
            distanceY = centerY;
            targetY = 0;
        }
        else {
            distanceY = screenSize.height - centerY;
            targetY = screenSize.height;
        }

        // Now closest to a vertical or horizontal border
        if (distanceX < distanceY) {
            // Closest to vertical border
            // Keep Y unchanged unless too close to corner
            targetY = Math.min(Math.max(centerY, SCREEN_CORNER_DEAD_ZONE_Y_PIXELS), screenSize.height - SCREEN_CORNER_DEAD_ZONE_Y_PIXELS);
        }
        else {
            // Closest to horizontal border
            // Keep X unchanged unless too close to corner
            targetX = Math.min(Math.max(centerX, SCREEN_CORNER_DEAD_ZONE_X_PIXELS), screenSize.width - SCREEN_CORNER_DEAD_ZONE_X_PIXELS);
        }
        computeButtonPositions(targetX, targetY);

        return new Point(targetX - window.getWidth() / 2, targetY - window.getHeight() / 2);
    }

    // This fills up the deltasByPosAndSize array each time the window is move so that paintComponent() does not have to compute relative positions them over and over avain
    private void computeButtonPositions(int x, int y) {
        if (y == 0) {
            // TOP
            deltasByPosAndSize[0][LARGE] = new Point(OFFSET_X_LARGE - LARGE_RADIUS_PIXELS_DIAG, OFFSET_Y_LARGE + LARGE_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[0][MEDIUM] = new Point(OFFSET_X_MEDIUM - MEDIUM_RADIUS_PIXELS_DIAG, OFFSET_Y_MEDIUM + MEDIUM_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[0][SMALL] = new Point(OFFSET_X_SMALL - SMALL_RADIUS_PIXELS_DIAG, OFFSET_Y_SMALL + SMALL_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[1][LARGE] = new Point(OFFSET_X_LARGE, OFFSET_Y_LARGE + LARGE_RADIUS_PIXELS);
            deltasByPosAndSize[1][MEDIUM] = new Point(OFFSET_X_MEDIUM, OFFSET_Y_MEDIUM + MEDIUM_RADIUS_PIXELS);
            deltasByPosAndSize[1][SMALL] = new Point(OFFSET_X_SMALL, OFFSET_Y_SMALL + SMALL_RADIUS_PIXELS);
            deltasByPosAndSize[2][LARGE] = new Point(OFFSET_X_LARGE + LARGE_RADIUS_PIXELS_DIAG, OFFSET_Y_LARGE + LARGE_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[2][MEDIUM] = new Point(OFFSET_X_MEDIUM + MEDIUM_RADIUS_PIXELS_DIAG, OFFSET_Y_MEDIUM + MEDIUM_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[2][SMALL] = new Point(OFFSET_X_SMALL + SMALL_RADIUS_PIXELS_DIAG, OFFSET_Y_SMALL + SMALL_RADIUS_PIXELS_DIAG);
        }
        else if (y == screenSize.height) {
            // BOTTOM
            deltasByPosAndSize[0][LARGE] = new Point(OFFSET_X_LARGE - LARGE_RADIUS_PIXELS_DIAG, OFFSET_Y_LARGE - LARGE_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[0][MEDIUM] = new Point(OFFSET_X_MEDIUM - MEDIUM_RADIUS_PIXELS_DIAG, OFFSET_Y_MEDIUM - MEDIUM_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[0][SMALL] = new Point(OFFSET_X_SMALL - SMALL_RADIUS_PIXELS_DIAG, OFFSET_Y_SMALL - SMALL_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[1][LARGE] = new Point(OFFSET_X_LARGE, OFFSET_Y_LARGE - LARGE_RADIUS_PIXELS);
            deltasByPosAndSize[1][MEDIUM] = new Point(OFFSET_X_MEDIUM, OFFSET_Y_MEDIUM - MEDIUM_RADIUS_PIXELS);
            deltasByPosAndSize[1][SMALL] = new Point(OFFSET_X_SMALL, OFFSET_Y_SMALL - SMALL_RADIUS_PIXELS);
            deltasByPosAndSize[2][LARGE] = new Point(OFFSET_X_LARGE + LARGE_RADIUS_PIXELS_DIAG, OFFSET_Y_LARGE - LARGE_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[2][MEDIUM] = new Point(OFFSET_X_MEDIUM + MEDIUM_RADIUS_PIXELS_DIAG, OFFSET_Y_MEDIUM - MEDIUM_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[2][SMALL] = new Point(OFFSET_X_SMALL + SMALL_RADIUS_PIXELS_DIAG, OFFSET_Y_SMALL - SMALL_RADIUS_PIXELS_DIAG);
        }
        else if (x == 0) {
            // LEFT
            deltasByPosAndSize[0][LARGE] = new Point(OFFSET_X_LARGE + LARGE_RADIUS_PIXELS_DIAG, OFFSET_Y_LARGE - LARGE_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[0][MEDIUM] = new Point(OFFSET_X_MEDIUM + MEDIUM_RADIUS_PIXELS_DIAG, OFFSET_Y_MEDIUM - MEDIUM_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[0][SMALL] = new Point(OFFSET_X_SMALL + SMALL_RADIUS_PIXELS_DIAG, OFFSET_Y_SMALL - SMALL_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[1][LARGE] = new Point(OFFSET_X_LARGE + LARGE_RADIUS_PIXELS, OFFSET_Y_LARGE);
            deltasByPosAndSize[1][MEDIUM] = new Point(OFFSET_X_MEDIUM + MEDIUM_RADIUS_PIXELS, OFFSET_Y_MEDIUM);
            deltasByPosAndSize[1][SMALL] = new Point(OFFSET_X_SMALL + SMALL_RADIUS_PIXELS, OFFSET_Y_SMALL);
            deltasByPosAndSize[2][LARGE] = new Point(OFFSET_X_LARGE + LARGE_RADIUS_PIXELS_DIAG, OFFSET_Y_LARGE + LARGE_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[2][MEDIUM] = new Point(OFFSET_X_MEDIUM + MEDIUM_RADIUS_PIXELS_DIAG, OFFSET_Y_MEDIUM + MEDIUM_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[2][SMALL] = new Point(OFFSET_X_SMALL + SMALL_RADIUS_PIXELS_DIAG, OFFSET_Y_SMALL + SMALL_RADIUS_PIXELS_DIAG);
        }
        else {
            // RIGHT
            deltasByPosAndSize[0][LARGE] = new Point(OFFSET_X_LARGE - LARGE_RADIUS_PIXELS_DIAG, OFFSET_Y_LARGE - LARGE_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[0][MEDIUM] = new Point(OFFSET_X_MEDIUM - MEDIUM_RADIUS_PIXELS_DIAG, OFFSET_Y_MEDIUM - MEDIUM_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[0][SMALL] = new Point(OFFSET_X_SMALL - SMALL_RADIUS_PIXELS_DIAG, OFFSET_Y_SMALL - SMALL_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[1][LARGE] = new Point(OFFSET_X_LARGE - LARGE_RADIUS_PIXELS, OFFSET_Y_LARGE);
            deltasByPosAndSize[1][MEDIUM] = new Point(OFFSET_X_MEDIUM - MEDIUM_RADIUS_PIXELS, OFFSET_Y_MEDIUM);
            deltasByPosAndSize[1][SMALL] = new Point(OFFSET_X_SMALL - SMALL_RADIUS_PIXELS, OFFSET_Y_SMALL);
            deltasByPosAndSize[2][LARGE] = new Point(OFFSET_X_LARGE - LARGE_RADIUS_PIXELS_DIAG, OFFSET_Y_LARGE + LARGE_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[2][MEDIUM] = new Point(OFFSET_X_MEDIUM - MEDIUM_RADIUS_PIXELS_DIAG, OFFSET_Y_MEDIUM + MEDIUM_RADIUS_PIXELS_DIAG);
            deltasByPosAndSize[2][SMALL] = new Point(OFFSET_X_SMALL - SMALL_RADIUS_PIXELS_DIAG, OFFSET_Y_SMALL + SMALL_RADIUS_PIXELS_DIAG);
        }
    }


    ////////////////////////////
    // EVENT HANDLERS

    private void onCapture() {
        CaptureSelectionWindow captureSelectionWindow = new CaptureSelectionWindow();
    }


    private void onHistory() {
        try {
            Robot robot = new Robot();

            Rectangle rectangle = new Rectangle(screenSize);
            BufferedImage bufferedImage = robot.createScreenCapture(rectangle);
            File file = new File("screen-capture.png");
            if (ImageIO.write(bufferedImage, "png", file)) {
                JOptionPane.showMessageDialog(null, "File:- " + file.getAbsolutePath(), "Screen Captured", JOptionPane.INFORMATION_MESSAGE);
            }
            else {
                JOptionPane.showMessageDialog(null, "Capture failed (" + file.getAbsolutePath() + ")", "Screen capture error", JOptionPane.ERROR_MESSAGE);
            }
        }
        catch (AWTException | IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, ex.getMessage() + " - Full error on the console", "Screen capture error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void onMore() {
        JOptionPane.showMessageDialog(null, "This should open the more window - Now exiting...");
        System.exit(0);
    }

}