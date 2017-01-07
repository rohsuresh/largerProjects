package editor;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;

/**
 * Created by rohansuresh on 3/4/16.
 */
public class Rendering {

    public double cursorX;
    public double cursorY;
    Node temp;
    Node lastSpace;
    double margin;
    double down;

    public Rendering() {
        down = 0;
        margin = 5.0;
        cursorX = margin;
        cursorY = 0.0;
    }

    // keep track of the last space in a line
    // set that equal to lastSpace so that you only iterate through a line at a time

    public void render(double height, ArrayList<Holder> arr, double width, String fName, int fSize) {
        int line = 0;
        double yPos = 0.0;
        for (int i = 0; i < arr.size(); i++) {
            temp = arr.get(i).sentinel.next;
            Text itemToRender;
            double original = height;
            double widthSoFar = margin;
            double xPos = margin;
            //cursorY = i * height;
            while (temp != arr.get(i).sentinel) {
                itemToRender = temp.item;
                itemToRender.setFont((Font.font(fName, fSize)));
                if (itemToRender.getText().equals(" ")) {
                    lastSpace = temp.next;
                }
                if (lastSpace == null) {
                    if (widthSoFar + itemToRender.getLayoutBounds().getWidth() <= width - margin) {
                        itemToRender.setX(xPos);
                        itemToRender.setY(yPos);
                        itemToRender.setTextOrigin(VPos.TOP);
                        xPos += itemToRender.getLayoutBounds().getWidth();
                        yPos = yPos;
                        //cursorX = Math.round(xPos);
                        //cursorY = yPos;
                        widthSoFar += itemToRender.getLayoutBounds().getWidth();
                    } else {
                        xPos = 5.0;
                        line++;
                        yPos += height;
                        itemToRender.setTextOrigin(VPos.TOP);
                        widthSoFar = margin;
                        itemToRender.setX(xPos);
                        itemToRender.setY(yPos);
                        xPos+= itemToRender.getLayoutBounds().getWidth();
                        //cursorX = Math.round(xPos);
                        //cursorY = yPos;
                        widthSoFar += itemToRender.getLayoutBounds().getWidth();
                    }
                }
                else if (lastSpace != null) {
                    if (widthSoFar + itemToRender.getLayoutBounds().getWidth() <= width - margin) {
                        yPos = yPos;
                        itemToRender.setX(xPos);
                        itemToRender.setY(yPos);
                        itemToRender.setTextOrigin(VPos.TOP);
                        xPos += itemToRender.getLayoutBounds().getWidth();
                        widthSoFar += itemToRender.getLayoutBounds().getWidth();
                    } else {
                        //System.out.println(lastSpace.item);
                        double sinceLastSpace = widthSoFar + itemToRender.getLayoutBounds().getWidth() - lastSpace.item.getX();
                        xPos = margin;
                        line++;
                        yPos += height;
                        while (lastSpace != arr.get(i).sentinel) {
                            lastSpace.item.setX(xPos);
                            lastSpace.item.setY(yPos);
                            //System.out.println(yPos);
                            xPos += lastSpace.item.getLayoutBounds().getWidth();
                            widthSoFar += lastSpace.item.getLayoutBounds().getWidth();
                            lastSpace = lastSpace.next;
                        }
                        xPos = margin + sinceLastSpace;
                        //cursorX = Math.round(xPos);
                        //cursorY = yPos;
                        widthSoFar = margin + sinceLastSpace;
                        itemToRender.setTextOrigin(VPos.TOP);
                        lastSpace = null;
                    }
                }
                temp = temp.next;
            }
            yPos += height;
        }
        line++;

        /*if (!arr.get(num).isEmpty() && arr.get(i).cursorSentinel != arr.get(i).sentinel) {
            cursorX = Math.round(arr.get(i).cursorSentinel.item.getX()) +
                    Math.round(arr.get(i).cursorSentinel.item.getLayoutBounds().getWidth());
        }
        else {
            cursorX = margin;
        }*/


    }

    public void renderBackspace(ArrayList<Holder> a, Group rt, int index) {
        //System.out.println(index);
        if (a.get(index).cursorSentinel == a.get(index).sentinel && index == 0) {
            cursorX = margin;
        }
        else if (a.get(index).size() == 1 && index != 0) {
            //System.out.println(index);
            rt.getChildren().remove(a.get(index).remove());
            index--;
            //System.out.println(a.size());
            a.remove(index + 1);
            //System.out.println(a.size());
            //index --;
            //System.out.println(index);
            a.get(index).cursorSentinel = a.get(index).sentinel.prev;
            //System.out.println("back");
            //maxLine = a.size() - 1;
        } else if (a.get(index).size() > 0) {
            Text last = a.get(index).remove();
            rt.getChildren().remove(last);
            if (a.get(index).cursorSentinel != a.get(index).sentinel) {
                cursorX = Math.round(a.get(index).cursorSentinel.item.getX()) +
                        Math.round(a.get(index).cursorSentinel.item.getLayoutBounds().getWidth());
                cursorY = a.get(index).cursorSentinel.item.getY();
                //System.out.println(cursorY);
            } else if (a.get(index).size() == 0) {
                cursorX = margin;
            } else {
                cursorX = margin;
            }
            //maxLine = a.size() - 1;
        }
    }

    public void moveLeft(ArrayList<Holder> arr, int num, double height) {
        if (arr.get(num).cursorSentinel == arr.get(num).sentinel && num == 0) {
        } else if (arr.get(num).cursorSentinel ==  arr.get(num).sentinel && num > 0) {
            num --;
            //System.out.println("here");
            cursorX = Math.round(arr.get(num).sentinel.prev.item.getX()) +
                    Math.round(arr.get(num).sentinel.prev.item.getLayoutBounds().getWidth());
            cursorY = arr.get(num).sentinel.prev.item.getY();
        }
        else if (arr.get(num).cursorSentinel.prev != arr.get(num).sentinel) {
            if (arr.get(num).cursorSentinel.item.getX() == 5.0) {
                //System.out.println(cursorX);
                if (num > 0) {
                    num--;
                }
                arr.get(num).cursorSentinel = arr.get(num).cursorSentinel.prev;
                cursorX = Math.round(arr.get(num).cursorSentinel.item.getX()) +
                        Math.round(arr.get(num).cursorSentinel.item.getLayoutBounds().getWidth());
                cursorY = arr.get(num).cursorSentinel.item.getY();
            } else {
                arr.get(num).cursorSentinel = arr.get(num).cursorSentinel.prev;
                cursorX = Math.round(arr.get(num).cursorSentinel.item.getX()) +
                        Math.round(arr.get(num).cursorSentinel.item.getLayoutBounds().getWidth());
            }
        } else {
            cursorX = 5.0;
            arr.get(num).cursorSentinel = arr.get(num).sentinel;
        }

    }

    public void moveRight(ArrayList<Holder> arr, int num, double height) {
        double original = height;
        double y = height;
        if (arr.get(num).size() == 0 && num == 0) {

        }
        else if (arr.get(num).cursorSentinel.next != arr.get(num).sentinel) {
            arr.get(num).cursorSentinel = arr.get(num).cursorSentinel.next;
            cursorX = Math.round(arr.get(num).cursorSentinel.item.getX()) +
                    Math.round(arr.get(num).cursorSentinel.item.getLayoutBounds().getWidth());
            cursorY = arr.get(num).cursorSentinel.item.getY();
        } else if (arr.get(num).cursorSentinel == arr.get(num).sentinel && num < arr.size() - 1) {
            if (arr.get(num).size() == 1) {
                num++;
                cursorX = 5.0;
                cursorY = arr.get(num).cursorSentinel.item.getY();
            }
        }

        else {
            cursorX = Math.round(arr.get(num).cursorSentinel.item.getX()) +
                    Math.round(arr.get(num).cursorSentinel.item.getLayoutBounds().getWidth());
        }
    }

    public void moveUp(ArrayList<Holder> arr, int ln, double height) {
        // if lineNumber is 0 and there are no lines above it (top line of wrap) do nothing
        if (cursorY > 0) {
            // do nothing, can't go up more
            cursorX = cursorX;
            cursorY = cursorY - height;
        }
        // otherwise see if line above it has same lineNumber of diff lineNumber
    }

    public void moveDown(ArrayList<Holder> arr, int ln, double height) {
        double maxLow = arr.get(ln).cursorSentinel.item.getY();
        if (cursorY == maxLow) {
            // do nothing, can't go down more
        } else {
            cursorX = cursorX;
            cursorY += height;
        }
    }
}
