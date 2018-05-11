/**
 * @author Maxence Weyrich
 * @version 5/11/2018
 *
 *
 * A quick note about the format of files:
 *
 * the input files must have all the points in X Y order with a space separating all data points (by default).
 * All data must appear in pairs; an odd number of data points will cause an error to be generated.
 * Custom format can also be specified, see below for more information.
 *
 *
 * The program also accepts an optional format specifier in the following formats:
 *
 * color can be specified with : {R,G,B}
 * where R, G, B are integer values between 0-255 representing red, green and blue, respectively
 *
 * size can be specified with : [S]
 * where S is a positive integer. (behavior unspecified with negative values)
 *
 * Default color is black, and default size is 7.
 *
 * Note that these formatting options must appear immediately after the Y coordinate, and there must be
 * whitespace separating each specifier.
 *
 * Sample input files are available to see and try at https://github.com/maxwey/FilePlotter/tree/master/examples
 *
 *
 *
 * Format specifiers at the top of the input files to indicate the structure of the following file are optional
 * but can be specified for non-conventional file formats. All header format specifers must occur at the very
 * top of the file, and start with double pound symbols (##)
 *
 * Available specifiers include:
 * FORMAT: %x %y
 *    The text following the FORMAT can be anything, but must include the %x and the %y, as these indicate
 *    where in the text the x and y coordinates are located. Note that is it not recommended to have the
 *    same format as the color/size specifiers as it may cause parsing conflicts
 *
 *    For example: a format of (%y...%x) would indicate that the data (5...3)
 *    would represent an x-coordinate of 3 and a y-coordinate of 5
 *
 * SIZEDEFAULT: S
 *    SIZEDEFAULT must be followed by a non-negative integer, S, that specifies the default size of the
 *    points in the graph. This can still be overridden by the [##] format specifiers
 *
 * COLORDEFAULT: {R,G,B}
 *    COLORDEFAULT must be followed by R,G,B values where R, G, and B are integer values between 0 and 255,
 *    where each number represents red, green and blue. The required format is the same as the one for
 *    inline format specifiers
 *
 */

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.swing.*;
import java.awt.*;

public class Plotter {

   private JPanel panel;
   private ArrayList<DrawableItem> itemsToDraw;

   private int defaultSize = 7;
   private Color defaultColor = Color.BLACK;
   private Pattern defaultData = Pattern.compile("(?<x>.+?)\\s+(?<y>.+?)(?:\\s+(?<F>.*)|$)");

   // create the Reg-ex search patterns for the file fomatting
   private static final Pattern POINT_COLOR_PATTERN = Pattern.compile("\\{(\\d{1,3}),(\\d{1,3}),(\\d{1,3})\\}");
   private static final Pattern POINT_SIZE_PATTERN = Pattern.compile("\\[(\\d+)\\]");
   private static final Pattern HEADER_PATTERN = Pattern.compile("##.*");
   private static final Pattern FORMAT_EXTRACT_PATTERN = Pattern.compile("##(.+):(.+)");


   public static void main(String[] args) throws FileNotFoundException {
      if(args.length < 1) throw new IllegalArgumentException("Expected filename in arguments");
      new Plotter(args[0]);
   }

   public Plotter(String fileName) throws FileNotFoundException {
      JFrame frame = new JFrame("Plotter");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      JLabel xAxis = new JLabel("X-axis");
      JLabel yAxis = new JLabel("Y-axis");

      //create a new JPanel with an overridden paintComponent method to call the paintUpdate method
      panel = new JPanel() {
         @Override
         protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            paintUpdate((Graphics2D)g);
         }
      };
      frame.setContentPane(panel);

      frame.setSize(1000, 600);
      frame.setVisible(true);

      //create the array of items that will be drawn, and start scanning/parsing the input file
      itemsToDraw = new ArrayList<DrawableItem>();
      start(new Scanner(new File(fileName)));
   }

   /*
    * Parses the given key & value and sets the appropriate settings based on the values
    * read.
    * Currently returns nothing (has no effect :^( )
    */
   private void parseFormat(String key, String value) {
      key = key.trim().toUpperCase();

      if(key.equals("FORMAT")) { // User has specified a custom file formatting
         value = Pattern.quote(value);
         value = value.replaceFirst("%x", "\\\\E(?<x>.+?)\\\\Q");
         value = value.replaceFirst("%y", "\\\\E(?<y>.+?)\\\\Q");
         value += "(?:\\s+(?<F>.*)|$)";
         this.defaultData = Pattern.compile(value);
         //System.out.println(value);

      } else if(key.equals("SIZEDEFAULT")) { // User has specified as custom default size
         this.defaultSize = Integer.parseInt(value);
      } else if(key.equals("COLORDEFAULT")) {
         Matcher matcher = POINT_COLOR_PATTERN.matcher(value);
         matcher.find();

         int r = Integer.parseInt(matcher.group(1));
         int g = Integer.parseInt(matcher.group(2));
         int b = Integer.parseInt(matcher.group(3));

         this.defaultColor = new Color(r, g, b);
      }

   }



   /**
    * Parses the given file using the given Scanner, and creates the basic items needed for the plotter including:
    * - Axis
    * - Points
    * - Format definitions
    *
    * Files must be formatted appropritately (as indicated by comment at top of file) or an IllegalArgumentException will
    * be thrown at runtime.
    *
    * @param Scanner scan the scanner of the file to read to add the points to plot
    */
   public void start(Scanner scan) {

      Axis ax = new Axis(0, 0, 0, 0, Color.BLACK, panel);
      ax.setRightOffset(20);
      ax.setLeftOffset(20);
      ax.setBottomOffset(20);
      ax.setTopOffset(30);
      itemsToDraw.add(ax);

      scan.useDelimiter("(\r)*(\n)+");


      //process header
      while(scan.hasNext(FORMAT_EXTRACT_PATTERN)) {

            String headerData = scan.next(FORMAT_EXTRACT_PATTERN);
            Matcher matcher = FORMAT_EXTRACT_PATTERN.matcher(headerData);
            matcher.find();
            parseFormat(matcher.group(1).trim(), matcher.group(2).trim());

      }


      //scan.useDelimiter("\\s+");
      //process data
      ArrayList<Point> points = new ArrayList<Point>();
      while(scan.hasNext()) {
         try {
            //default color is black
            Color c = this.defaultColor;
            //default point size is 7
            int size = this.defaultSize;


            String in = scan.next();

            Matcher pointMatcher = this.defaultData.matcher(in);
            if(!pointMatcher.find()) {
               continue;
            }

            String format = pointMatcher.group("F");

            double x = Double.parseDouble(pointMatcher.group("x"));
            double y = Double.parseDouble(pointMatcher.group("y"));


            if(format != null) {

               Matcher colorMatcher = POINT_COLOR_PATTERN.matcher(format);
               if(colorMatcher.find()) {
                  int r = Integer.parseInt(colorMatcher.group(1));
                  int g = Integer.parseInt(colorMatcher.group(2));
                  int b = Integer.parseInt(colorMatcher.group(3));
                  c = new Color(r, g, b);
               }

               Matcher sizeMatcher = POINT_SIZE_PATTERN.matcher(format);
               if(sizeMatcher.find()) {
                  size = Integer.parseInt(sizeMatcher.group(1));
               }
            }

            points.add(new Point(x, y, size, c, ax));
         } catch (Exception e) {
            scan.close();
            System.out.println(e.toString());
            e.printStackTrace();
            throw new IllegalArgumentException("Malformed file");
         }
      }
      scan.close();

      //find max x & max y to indicate axis the size
      double maxX = 1, maxY = 1;
      for(Point p : points) {
         if(p.getX() > maxX) {
            maxX = p.getX();
         }
         if(p.getY() > maxY) {
            maxY = p.getY();
         }
         itemsToDraw.add(p);
      }
      maxX = Math.ceil(maxX);
      maxY = Math.ceil(maxY);
      ax.setXMax((int)maxX);
      ax.setYMax((int)maxY);
      ax.setXScale(Math.max(1, (int)(maxX/20)));
      ax.setYScale(Math.max(1, (int)(maxY/20)));

      //sort items to draw by z-index value (items with a higher z-index are "higher" and thus get drawn last)
      Collections.sort(itemsToDraw);


      panel.repaint();
   }

   /**
    * Called every time the panel needs to be repainted
    * @param Graphics g the Graphics object
    */
   public void paintUpdate(Graphics2D g) {

      Collections.sort(itemsToDraw);
      for(DrawableItem d : itemsToDraw) {
         d.draw(g);
      }

   }



   //===================================================================================
   //===================================================================================

   /**
    * The DrawableItem item class provides a basis for all drawable items such as the axis,
    * and the points.
    *
    * The abstract class implements Comparable, returning the order of the items
    * based on their z-indicies in order to determine draw order.
    *
    * Should DrawableItem items require a different ordering, a separate Comparator object should be defined.
    */
   private abstract class DrawableItem implements Comparable<DrawableItem> {
      private int zVal;

      public DrawableItem(int z) {
         zVal = z;
      }

      public int getZVal() {
         return zVal;
      }

      public void setZVal(int val) {
         zVal = val;
      }

      /**
       * Comapare this DrawableItem with the other to deterimine which gets drawn first (on top)
       * @param  DrawableItem other        the other drawable item
       * @return              < 0 if this item comes before the other (is on top), 0 if equal (will draw in order created), > 0 otherwise
       */
      public int compareTo(DrawableItem other) {
         return zVal - other.zVal;
      }

      public abstract void draw(Graphics2D g);
   }


   /**
    * Point class represents a single point object in the plotter.
    * Each individual point must have a unique object, and must be specified with a
    * color, size, location and a reference to the Axis this point will be plotted on.
    *
    * The Axis reference is required so that the point knows where the axis is located on
    * the JPanel (e.g. it may be offset from the JPanel for cosmetic purposes) and where
    * it should draw itself.
    */
   private class Point extends DrawableItem {
      private Axis axis;
      private double x;
      private double y;
      private Color color;
      private int size;

      /**
       * Creates a point at the specified x and y values.
       * Note that the x and y values are based on the axis values.
       * @param  double x             X-axis value
       * @param  double y             y-axis values
       * @param  int size             size of the point to draw
       * @param  Color color          the color of the point to draw
       * @param  int z             z-index value for drawing
       * @param  Axis axis         the axis this point is being drawn in
       */
      public Point(double x, double y, int size, Color color, int z, Axis axis) {
         super(z);
         this.x = x;
         this.y = y;
         this.axis = axis;
         this.color = color;
         this.size = size;
      }

      /**
       * Creates a point at the specified x and y values.
       * Note that the x and y values are based on the axis values.
       * The Z-index defaults to 100
       * @param  double x             X-axis value
       * @param  double y             y-axis values
       * @param  int size             size of the point to draw
       * @param  Color color          the color of the point to draw
       * @param  Axis axis         the axis this point is being drawn in
       */
      public Point(double x, double y, int size, Color color, Axis axis) {
         super(100);
         this.x = x;
         this.y = y;
         this.axis = axis;
         this.color = color;
         this.size = size;
      }

      /**
       * Uses the Graphics object to draw the current Point
       * Note that you still need to draw the graphics object!
       * @param Graphics g graphics to use.
       */
      public void draw(Graphics2D g) {
         Color curr = g.getColor();
         g.setColor(color);

         Rectangle rect = axis.getAxisBounds(null);

         double xloc = ((double)rect.width)/axis.getXMax();
         double yloc = ((double)rect.height)/axis.getYMax();

         g.fillOval((int)(x*xloc)+rect.x-size/2, (int)(axis.getYMax()*yloc-(y*yloc))+rect.y-size/2, size, size);
         g.setColor(curr);
      }

      public void setX(double val) {
         x = val;
      }

      public void setY(double val) {
         y = val;
      }

      public double getX() {
         return x;
      }

      public double getY() {
         return y;
      }

      /**
       * Sets the axis to the given axis if non-null.
       * Gets the current axis if given axis is null
       * @param  Axis ax            the new axis (or null to get)
       * @return      the current axis
       */
      public Axis setGetAxis(Axis ax) {
         if(ax != null) axis = ax;
         return axis;
      }
   }


   /**
    * The Axis class represents an Axis object, which retains information such as
    * - where on the JPanel the axis is located (including offsets), as well as the JPanel itself
    * - the color to be drawn in
    * - the scale of the axis
    * - the y & x maximums of the axis
    *
    * All objects that must be plotted based on the location of the axis should use
    * and axis object rather than the JPanel object as the axis may vary in scale and offsets.
    */
   private class Axis extends DrawableItem {

      private int rOffset;
      private int lOffset;
      private int bOffset;
      private int tOffset;
      private int xMax;
      private int yMax;
      private int xScale;
      private int yScale;
      private Color color;
      private JPanel panel;

      /**
       * Creates an Axis based on the following information
       * @param  int    xMax        the max X value
       * @param  int    yMax        the max Y value
       * @param  int    xScale      the scale to use for the X-axis
       * @param  int    yScale      the scale to use for the Y-axis
       * @param  int    z             the z-index (for drawing)
       * @param  Color  color         the color of the Axis
       * @param  JPanel panel         the panel the axis is being drawn into
       */
      public Axis(int xMax, int yMax, int xScale, int yScale, int z, Color color, JPanel panel) {
         super(z);
         this.xMax = xMax;
         this.yMax = yMax;
         this.xScale = xScale;
         this.yScale = yScale;
         this.color = color;
         this.panel = panel;
      }

      /**
       * Creates an Axis based on the following information
       * Uses a default z-index value of 100
       * @param  int    xMax        the max X value
       * @param  int    yMax        the max Y value
       * @param  int    xScale       the scale to use on the X-axis
       * @param  int    yScale       the scale to use on the Y-axis
       * @param  Color  color         the color of the Axis
       * @param  JPanel panel         the panel the axis is being drawn into
       */
      public Axis(int xMax, int yMax, int xScale, int yScale, Color color, JPanel panel) {
         super(100);
         this.xMax = xMax;
         this.yMax = yMax;
         this.xScale = xScale;
         this.yScale = yScale;
         this.color = color;
         this.panel = panel;
      }

      public void draw(Graphics2D g) {
         Color currC = g.getColor();
         Stroke currS = g.getStroke();
         g.setColor(color);

         Rectangle rect = getAxisBounds(null);

         g.setStroke(new BasicStroke(2));
         g.drawLine(rect.x, rect.y, rect.x, rect.y+rect.height);
         g.drawLine(rect.x, rect.y+rect.height, rect.x+rect.width, rect.y+rect.height);

         double disp = rect.height/(yMax/(double)yScale);
         for(int i = 0; i < yMax; i+=yScale) {
            g.drawString(""+(yMax - i), rect.x+10, (int)(rect.y+(i/yScale*disp)+3));
            g.drawLine(rect.x, (int)(rect.y+(i/yScale*disp)), rect.x+5, (int)(rect.y+(i/yScale*disp)));
         }
         disp = rect.width/(xMax/(double)xScale);
         for(int i = 0; i < xMax; i+=xScale) {
            g.drawString(""+(xMax - i), (int)(rect.x+rect.width-(i/xScale*disp)-3), rect.y + rect.height+15);
            g.drawLine((int)(rect.x+rect.width-(i/xScale*disp)), rect.y+rect.height, (int)(rect.x+rect.width-(i/xScale*disp)), rect.y+rect.height-5);
         }

         g.setColor(currC);
         g.setStroke(currS);
      }

      /**
       * Get a rectangle that encloses the Axis based on the x & y values of the JPanel the
       * axis is contained in.
       *
       * @param  Rectangle rect the rectangle to modify. Passing null will create a new Rectangle
       * @return the rectangle object
       */
      public Rectangle getAxisBounds(Rectangle rect) {
         rect = panel.getBounds(rect);
         rect.x = rect.x + lOffset;
         rect.width = (rect.width - lOffset) - rOffset;
         rect.y = rect.y + tOffset;
         rect.height = (rect.height - tOffset) - bOffset;
         return rect;
      }

      public int getXMax(){
         return xMax;
      }

      public int getYMax() {
         return yMax;
      }

      public void setXMax(int xMax) {
         this.xMax = xMax;
      }

      public void setYMax(int yMax) {
         this.yMax = yMax;
      }

      public int getXScale() {
         return this.xScale;
      }

      public int getYScale() {
         return this.yScale;
      }

      public void setXScale(int xScale) {
         this.xScale = xScale;
      }

      public void setYScale(int yScale) {
         this.yScale = yScale;
      }
      /**
       * Set the offset values. Will apply only when used to draw the axis in a frame.
       */
      public void setRightOffset(int val) {
         rOffset = val;
      }

      public void setLeftOffset(int val) {
         lOffset = val;
      }

      public void setTopOffset(int val) {
         tOffset = val;
      }

      public void setBottomOffset(int val) {
         bOffset = val;
      }
   }
}
