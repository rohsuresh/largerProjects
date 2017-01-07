package editor;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.control.ScrollBar;

import java.util.ArrayList;
import java.util.Stack;


/**
 * A JavaFX application that displays the letter the user has typed most recently in the center of
 * the window. Pressing the up and down arrows causes the font size to increase and decrease,
 * respectively.
 */
public class Editor extends Application {
    private final Rectangle textBoundingBox;
    private static int WINDOW_WIDTH = 500;
    private static int WINDOW_HEIGHT = 500;
    public double widthUsed;
    public Group textRoot;
    public Group root;
    public double cursorX;
    public double cursorY;
    public double cursorHeight;
    public int lineNumber;
    public int line;
    int maxLine;
    public Rendering r;
    public OpenSave os;
    private static String inputName;
    public String fontName;
    public int fontSize;
    public Stack undoStack;
    public Stack redoStack;

    Holder buffer = new Holder();
    ArrayList<Holder> grid;

    public Editor() {
        textBoundingBox = new Rectangle(5, 0);
        textRoot = new Group();
        root = new Group();
        root.getChildren().add(textRoot);
        grid = new ArrayList<Holder>();
        grid.add(buffer);
        cursorX = 5.0;
        cursorY = Math.round(lineNumber * cursorHeight);
        lineNumber = 0;
        line = 1;
        r = new Rendering();
        fontName = "Verdana";
        undoStack = new Stack();
        redoStack = new Stack();
    }

    /**
     * An EventHandler to handle keys that get pressed.
     */
    public class KeyEventHandler implements EventHandler<KeyEvent> {
        int textCenterX;
        int textCenterY;

        public static final int STARTING_FONT_SIZE = 12;
        private static final int STARTING_TEXT_POSITION_X = 5;
        private static final int STARTING_TEXT_POSITION_Y = 0;

        /**
         * The Text to display on the screen.
         */
        private Text displayText = new Text(STARTING_TEXT_POSITION_X, STARTING_TEXT_POSITION_Y, "");


        KeyEventHandler(final Group root, int windowWidth, int windowHeight) {
            textCenterX = windowWidth / 2;
            textCenterY = windowHeight / 2;

            // Initialize some empty text and add it to root so that it will be displayed.
            displayText = new Text(textCenterX, textCenterY, "");
            // Always set the text origin to be VPos.TOP! Setting the origin to be VPos.TOP means
            // that when the text is assigned a y-position, that position corresponds to the
            // highest position across all letters (for example, the top of a letter like "I", as
            // opposed to the top of a letter like "e"), which makes calculating positions much
            // simpler!
            displayText.setTextOrigin(VPos.TOP);
            fontSize = STARTING_FONT_SIZE;
            displayText.setFont(Font.font(fontName, fontSize));
            cursorHeight = displayText.getLayoutBounds().getHeight();
            // All new Nodes need to be added to the root in order to be displayed.
            //root.getChildren().add(displayText);
            centerTextAndUpdateBoundingBox();
        }

        @Override
        public void handle(KeyEvent keyEvent) {

            if (keyEvent.isShortcutDown()) {
                if (keyEvent.getCode() == KeyCode.P) {
                    System.out.println((int) r.cursorX + ", " + (int) r.cursorY);
                } else if (keyEvent.getCode() == KeyCode.MINUS) {
                    if (fontSize == 4) {
                    } else {
                        fontSize -= 4;
                        cursorHeight -= 4;
                        r.render(cursorHeight, grid, widthUsed, fontName, fontSize);
                        r.cursorX = grid.get(lineNumber).cursorSentinel.item.getX() +
                                grid.get(lineNumber).cursorSentinel.item.getLayoutBounds().getWidth();
                        r.cursorY = grid.get(lineNumber).cursorSentinel.item.getY();
                        centerTextAndUpdateBoundingBox();
                    }
                } else if (keyEvent.getCode() == KeyCode.PLUS || keyEvent.getCode() == KeyCode.EQUALS) {
                    fontSize += 4;
                    cursorHeight += 4;
                    r.render(cursorHeight, grid, widthUsed, fontName, fontSize);
                    r.cursorX = grid.get(lineNumber).cursorSentinel.item.getX() +
                            grid.get(lineNumber).cursorSentinel.item.getLayoutBounds().getWidth();
                    r.cursorY = grid.get(lineNumber).cursorSentinel.item.getY();
                    centerTextAndUpdateBoundingBox();
                } else if (keyEvent.getCode() == KeyCode.Z) {
                    // undo
                    if (!undoStack.isEmpty()) {
                        if (undoStack.peek().equals("backspace")) {
                            if (grid.get(lineNumber).size() == 1 && lineNumber != 0) {
                                redoStack.push("enter");
                                r.renderBackspace(grid, textRoot, lineNumber);
                                lineNumber--;
                                r.render(cursorHeight, grid, widthUsed, fontName, fontSize);
                                centerTextAndUpdateBoundingBox();
                            } else {
                                Text item = grid.get(lineNumber).cursorSentinel.item;
                                r.renderBackspace(grid, textRoot, lineNumber);
                                r.render(cursorHeight, grid, widthUsed, fontName, fontSize);
                                centerTextAndUpdateBoundingBox();
                                redoStack.push(item);
                            }
                            undoStack.pop();
                        } else {
                            Text putBack = (Text) undoStack.pop();
                            redoStack.push("backspace");
                            grid.get(lineNumber).add(putBack);
                            //System.out.println(grid.get(lineNumber).size());
                            r.render(cursorHeight, grid, widthUsed, fontName, fontSize);
                            textRoot.getChildren().add(putBack);
                            r.cursorX = putBack.getX() + putBack.getLayoutBounds().getWidth();
                            r.cursorY = putBack.getY();
                            centerTextAndUpdateBoundingBox();
                            //keyEvent.consume();
                        }
                    }
                } else if (keyEvent.getCode() == KeyCode.Y) {
                    //System.out.println("Test");
                    // redo
                    if (!redoStack.isEmpty()) {
                        if (redoStack.peek().equals("backspace")) {
                            int sz = grid.size();
                            Text toDelete = grid.get(lineNumber).cursorSentinel.item;
                            r.renderBackspace(grid, textRoot, lineNumber);
                            r.render(cursorHeight, grid, widthUsed, fontName, fontSize);
                            centerTextAndUpdateBoundingBox();
                            /*if (grid.size() < sz) {
                                lineNumber--;

                            }*/
                            redoStack.pop();
                            undoStack.push(toDelete);
                        } else {
                            //System.out.println("Test");
                            Text again = (Text) redoStack.pop();
                            grid.get(lineNumber).add(again);
                            r.render(cursorHeight, grid, widthUsed, fontName, fontSize);
                            textRoot.getChildren().add(again);
                            r.cursorX = again.getX() + again.getLayoutBounds().getWidth();
                            r.cursorY = again.getY();
                            centerTextAndUpdateBoundingBox();
                            undoStack.push("backspace");
                        }
                    }
                } else if (keyEvent.getCode() == KeyCode.S) {
                    os.save();
                }
            }

                else if (keyEvent.getEventType() == KeyEvent.KEY_TYPED) {
                    // Use the KEY_TYPED event rather than KEY_PRESSED for letter keys, because with
                    // the KEY_TYPED event, javafx handles the "Shift" key and associated
                    // capitalization.
                    KeyCode key = keyEvent.getCode();
                    String characterTyped = keyEvent.getCharacter();
                    Text t = new Text(characterTyped);
                    t.setFont(Font.font(fontName, fontSize));

                    if (characterTyped.length() > 0 && characterTyped.charAt(0) != 8) {
                        // Ignore control keys, which have non-zero length, as well as the backspace
                        // key, which is represented as a character of value = 8 on Windows.
                        grid.get(lineNumber).add(t);
                        //maxLine = grid.size() - 1;
                        undoStack.push("backspace");
                        //System.out.println("Undo stack: " + undoStack.peek());
                        //System.out.println("Redo stack: " + redoStack.peek());
                        r.render(cursorHeight, grid, widthUsed, fontName, fontSize);
                        r.cursorX = t.getX() + t.getLayoutBounds().getWidth();
                        r.cursorY = t.getY();
                        textRoot.getChildren().add(t);
                        keyEvent.consume();
                    }

                    centerTextAndUpdateBoundingBox();


                } else if (keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
                    // Arrow keys should be processed using the KEY_PRESSED event, because KEY_PRESSED
                    // events have a code that we can check (KEY_TYPED events don't have an associated
                    // KeyCode).
                    KeyCode code = keyEvent.getCode();
                    if (code == KeyCode.BACK_SPACE) {
                        //Holder up = grid.get(lineNumber - 1);
                        if (grid.get(lineNumber).size() == 1 && lineNumber != 0) {
                            r.renderBackspace(grid, textRoot, lineNumber);
                            lineNumber--;
                            r.render(cursorHeight, grid, widthUsed, fontName, fontSize);
                        } else {
                            Text deleted = grid.get(lineNumber).cursorSentinel.item;
                            undoStack.push(deleted);
                            //System.out.println("Top of undo stack: " + (Text) undoStack.peek());
                            r.renderBackspace(grid, textRoot, lineNumber);
                            r.render(cursorHeight, grid, widthUsed, fontName, fontSize);
                        }
                        centerTextAndUpdateBoundingBox();
                    } else if (code == KeyCode.LEFT) {
                        r.moveLeft(grid, lineNumber, cursorHeight);
                        centerTextAndUpdateBoundingBox();
                    } else if (code == KeyCode.RIGHT) {
                        r.moveRight(grid, lineNumber, cursorHeight);
                        centerTextAndUpdateBoundingBox();
                    } else if (code == KeyCode.ENTER) {
                        Holder next = new Holder();
                        lineNumber++;
                        grid.add(lineNumber, next);
                        maxLine = grid.size() - 1;
                        //r.render(cursorHeight, grid, widthUsed, fontName, fontSize);
                        centerTextAndUpdateBoundingBox();
                    } else if (code == KeyCode.UP) {
                        r.moveUp(grid, lineNumber, cursorHeight);
                        centerTextAndUpdateBoundingBox();
                    } else if (code == KeyCode.DOWN) {
                        r.moveDown(grid, lineNumber, cursorHeight);
                        centerTextAndUpdateBoundingBox();
                    }
                }
            }
        }

    public void centerTextAndUpdateBoundingBox() {
        // Figure out the size of the current text.
        double cursorWidth = 1;

        // Calculate the position so that the text will be centered on the screen.
        double textTop = 0;
        double textLeft = 5;

        // Re-position the text.
        textBoundingBox.setX(textLeft);
        textBoundingBox.setY(textTop);

        // Re-size and re-position the bounding box.
        textBoundingBox.setHeight(cursorHeight);
        textBoundingBox.setWidth(cursorWidth);

        // For rectangles, the position is the upper left hand corner.
        textBoundingBox.setX(r.cursorX);
        textBoundingBox.setY(r.cursorY);
        // Many of the JavaFX classes have implemented the toString() function, so that
        // they print nicely by default.
        // System.out.println("Bounding box: " + textBoundingBox);

        // Make sure the text appears in front of the rectangle.
        textBoundingBox.toFront();
    }

    /*private void render() {
        int start = 0;
        int end = grid.get(lineNumber).size();
        Text itemToRender;
        double xPos = 5.0;
        cursorY = lineNumber * cursorHeight;
        double yPos = cursorY;
        while (start < end) {
            itemToRender = grid.get(lineNumber).get(start);
            itemToRender.setX(xPos);
            itemToRender.setY(yPos);
            xPos += itemToRender.getLayoutBounds().getWidth();
            itemToRender.setTextOrigin(VPos.TOP);
            start++;
        }

        if (!grid.get(lineNumber).isEmpty() && grid.get(lineNumber).cursorSentinel != grid.get(lineNumber).sentinel) {
            cursorX = Math.round(grid.get(lineNumber).cursorSentinel.item.getX()) +
                    Math.round(grid.get(lineNumber).cursorSentinel.item.getLayoutBounds().getWidth());
        }
        else {
            cursorX = 5.0;
        }
    }

    private void moveCursorLeft() {
        if (grid.get(lineNumber).cursorSentinel == grid.get(lineNumber).sentinel && lineNumber == 0) {
        } else if (grid.get(lineNumber).cursorSentinel.prev == grid.get(lineNumber).sentinel && lineNumber > 0) {
            lineNumber --;
            cursorX = Math.round(grid.get(lineNumber).cursorSentinel.item.getX()) +
                    Math.round(grid.get(lineNumber).cursorSentinel.item.getLayoutBounds().getWidth());
            cursorY = lineNumber * cursorHeight;
        } else if (grid.get(lineNumber).cursorSentinel.prev != grid.get(lineNumber).sentinel) {
            grid.get(lineNumber).cursorSentinel = grid.get(lineNumber).cursorSentinel.prev;

            cursorX = Math.round(grid.get(lineNumber).cursorSentinel.item.getX()) +
                    Math.round(grid.get(lineNumber).cursorSentinel.item.getLayoutBounds().getWidth());
        } else {
            cursorX = 5.0;
            grid.get(lineNumber).cursorSentinel = grid.get(lineNumber).sentinel;
        }
    }

    private void moveCursorRight() {
        if (grid.get(lineNumber).cursorSentinel.next != grid.get(lineNumber).sentinel) {
            grid.get(lineNumber).cursorSentinel = grid.get(lineNumber).cursorSentinel.next;
            cursorX = Math.round(grid.get(lineNumber).cursorSentinel.item.getX()) +
                    Math.round(grid.get(lineNumber).cursorSentinel.item.getLayoutBounds().getWidth());
        } else if (grid.get(lineNumber).cursorSentinel == grid.get(lineNumber).sentinel && lineNumber < maxLine) {
            if (grid.get(lineNumber).size() == 1) {
                lineNumber++;
                cursorX = 5.0;
                cursorY = lineNumber * cursorHeight;
            }
        }
        else if (grid.get(lineNumber).cursorSentinel.next == grid.get(lineNumber).sentinel && lineNumber < maxLine) {
            lineNumber++;
            cursorX = 5.0;
            cursorY = lineNumber * cursorHeight;
        }

        else {
            cursorX = Math.round(grid.get(lineNumber).cursorSentinel.item.getX()) +
                    Math.round(grid.get(lineNumber).cursorSentinel.item.getLayoutBounds().getWidth());
        }
    }*/

    public class RectangleBlinkEventHandler implements EventHandler<ActionEvent> {
        private int currentColorIndex = 0;
        private Color[] boxColors =
                {Color.WHITE, Color.BLACK};

        RectangleBlinkEventHandler() {
            // Set the color to be the first color in the list.
            changeColor();
        }

        private void changeColor() {
            textBoundingBox.setFill(boxColors[currentColorIndex]);
            currentColorIndex = (currentColorIndex + 1) % boxColors.length;
        }

        @Override
        public void handle(ActionEvent event) {
            changeColor();
        }
    }

    public void makeRectangleColorChange() {
        // Create a Timeline that will call the "handle" function of RectangleBlinkEventHandler
        // every 1 second.
        final Timeline timeline = new Timeline();
        // The rectangle should continue blinking forever.
        timeline.setCycleCount(Timeline.INDEFINITE);
        RectangleBlinkEventHandler cursorChange = new RectangleBlinkEventHandler();
        KeyFrame keyFrame = new KeyFrame(Duration.seconds(1), cursorChange);
        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    @Override
    public void start(Stage primaryStage) {
        // Create a Node that will be the parent of all things displayed on the screen.
        // The Scene represents the window: its height and width will be the height and width
        // of the window displayed.
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT, Color.WHITE);

        // To get information about what keys the user is pressing, create an EventHandler.
        // EventHandler subclasses must override the "handle" function, which will be called
        // by javafx.
        EventHandler<KeyEvent> keyEventHandler =
                new KeyEventHandler(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        // Register the event handler to be called for all KEY_PRESSED and KEY_TYPED events.
        scene.setOnKeyTyped(keyEventHandler);
        scene.setOnKeyPressed(keyEventHandler);

        textRoot.getChildren().add(textBoundingBox);
        makeRectangleColorChange();

        os = new OpenSave(grid, inputName, textRoot);
        os.open();

        grid.get(0).cursorSentinel = grid.get(0).sentinel;
        r.cursorX = 5;
        r.cursorY = 0;

        // Scroll bar initialized

        ScrollBar scrollBar = new ScrollBar();
        scrollBar.setOrientation(Orientation.VERTICAL);
        scrollBar.setMin(0);
        scrollBar.setPrefHeight(WINDOW_HEIGHT);
        scrollBar.setMax(WINDOW_HEIGHT);
        widthUsed = WINDOW_WIDTH - scrollBar.getLayoutBounds().getWidth();
        scrollBar.setLayoutX(widthUsed);

        // Resizing the window
        scene.widthProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldScreenWidth,
                    Number newScreenWidth) {
                // Re-compute window width.
                WINDOW_WIDTH = newScreenWidth.intValue();
                double newWidthUsed = WINDOW_WIDTH - scrollBar.getLayoutBounds().getWidth();
                r.render(cursorHeight, grid, newWidthUsed, fontName, fontSize);
                scrollBar.setLayoutX(newWidthUsed);
                if(grid.get(0).size() == 0 || grid.size() == 0 || grid.get(lineNumber).cursorSentinel == (grid.get(lineNumber).sentinel)) {
                    r.cursorX = 5;
                    r.cursorY = 0;
                } else {
                    r.cursorX = grid.get(lineNumber).cursorSentinel.item.getX() +
                            grid.get(lineNumber).cursorSentinel.item.getLayoutBounds().getWidth();

                    r.cursorY = grid.get(lineNumber).cursorSentinel.item.getY();
            }
                //scrollBar.setX(widthUsed);
                centerTextAndUpdateBoundingBox();
        }});

        scene.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldScreenHeight,
                    Number newScreenHeight) {
                WINDOW_HEIGHT = newScreenHeight.intValue();
                scrollBar.setPrefHeight(WINDOW_HEIGHT);
                scrollBar.setMax(WINDOW_HEIGHT);
        }});

        // insert scroll bar stuff
        //scrollBar.setPrefHeight(WINDOW_HEIGHT);
        //scrollBar.setMax(WINDOW_HEIGHT);
        //scrollbar.setValue(0);


        root.getChildren().add(scrollBar);

        scrollBar.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldValue, Number newValue) {
                textRoot.setLayoutY(-(double)newValue);
            }
        });

        r.render(cursorHeight, grid, widthUsed, fontName, fontSize);
        centerTextAndUpdateBoundingBox();

        primaryStage.setTitle("Editor");

        // This is boilerplate, necessary to setup the window where things are displayed.
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("No file name given.");
        } else if (args.length == 1) {
            inputName = args[0];
            launch(args);
        } else if (args.length == 2 && args[1].equals("debug")) {
            System.out.println("Debugger");
        }
    }
}
