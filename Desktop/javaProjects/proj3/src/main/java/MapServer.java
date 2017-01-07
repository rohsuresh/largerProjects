import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.io.IOException;
import java.util.Collections;
import java.util.ArrayList;
import java.awt.Graphics2D;
//import java.awt.Stroke;
import java.awt.BasicStroke;

/* Maven is used to pull in these dependencies. */
import com.google.gson.Gson;
//import com.sun.corba.se.impl.orbutil.graph.Graph;

import javax.imageio.ImageIO;

import static spark.Spark.*;

/**
 * This MapServer class is the entry point for running the JavaSpark web server for the BearMaps
 * application project, receiving API calls, handling the API call processing, and generating
 * requested images and routes.
 *
 * @author Alan Yao
 */
public class MapServer {
    /**
     * The root upper left/lower right longitudes and latitudes represent the bounding box of
     * the root tile, as the images in the img/ folder are scraped.
     * Longitude == x-axis; latitude == y-axis.
     */
    private static QuadTree qt;
    public static final double ROOT_ULLAT = 37.892195547244356, ROOT_ULLON = -122.2998046875,
            ROOT_LRLAT = 37.82280243352756, ROOT_LRLON = -122.2119140625;
    /**
     * Each tile is 256x256 pixels.
     */
    public static final int TILE_SIZE = 256;
    /**
     * HTTP failed response.
     */
    private static final int HALT_RESPONSE = 403;
    /**
     * Route stroke information: typically roads are not more than 5px wide.
     */
    public static final float ROUTE_STROKE_WIDTH_PX = 5.0f;
    /**
     * Route stroke information: Cyan with half transparency.
     */
    public static final Color ROUTE_STROKE_COLOR = new Color(108, 181, 230, 200);
    /**
     * The tile images are in the IMG_ROOT folder.
     */
    private static final String IMG_ROOT = "img/";
    /**
     * The OSM XML file path. Downloaded from <a href="http://download.bbbike.org/osm/">here</a>
     * using custom region selection.
     **/
    private static final String OSM_DB_PATH = "berkeley.osm";
    /**
     * Each raster request to the server will have the following parameters
     * as keys in the params map accessible by,
     * i.e., params.get("ullat") inside getMapRaster(). <br>
     * ullat -> upper left corner latitude,<br> ullon -> upper left corner longitude, <br>
     * lrlat -> lower right corner latitude,<br> lrlon -> lower right corner longitude <br>
     * w -> user viewport window width in pixels,<br> h -> user viewport height in pixels.
     **/
    private static final String[] REQUIRED_RASTER_REQUEST_PARAMS =
    {"ullat", "ullon", "lrlat", "lrlon", "w", "h"};
    /**
     * Each route request to the server will have the following parameters
     * as keys in the params map.<br>
     * start_lat -> start point latitude,<br> start_lon -> start point longitude,<br>
     * end_lat -> end point latitude, <br>end_lon -> end point longitude.
     **/
    private static final String[] REQUIRED_ROUTE_REQUEST_PARAMS =
    {"start_lat", "start_lon", "end_lat", "end_lon"};
    /* Define any static variables here. Do not define any instance variables of MapServer. */
    private static GraphDB g;
    private static LinkedList<Long> routeIDs = new LinkedList<>();
    private static LinkedList<GraphNode> routeNodes = new LinkedList<>();

    /**
     * Place any initialization statements that will be run before the server main loop here.
     * Do not place it in the main function. Do not place initialization code anywhere else.
     * This is for testing purposes, and you may fail tests otherwise.
     **/
    public static void initialize() {
        g = new GraphDB(OSM_DB_PATH);
        Node startRoot = new Node("", ROOT_ULLON, ROOT_ULLAT, ROOT_LRLON, ROOT_LRLAT, 0);
        qt = new QuadTree(startRoot);
    }

    public static QuadTree getQt() {
        return qt;
    }

    public static void main(String[] args) {
        initialize();
        staticFileLocation("/page");
        /* Allow for all origin requests (since this is not an authenticated server, we do not
         * care about CSRF).  */
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "*");
        });

        /* Define the raster endpoint for HTTP GET requests. I use anonymous functions to define
         * the request handlers. */
        get("/raster", (req, res) -> {
            HashMap<String, Double> params =
                    getRequestParams(req, REQUIRED_RASTER_REQUEST_PARAMS);
            /* The png image is written to the ByteArrayOutputStream */
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            /* getMapRaster() does almost all the work for this API call */
            Map<String, Object> rasteredImgParams = getMapRaster(params, os);
            /* On an image query success, add the image data to the response */
            if (rasteredImgParams.containsKey("query_success")
                    && (Boolean) rasteredImgParams.get("query_success")) {
                String encodedImage = Base64.getEncoder().encodeToString(os.toByteArray());
                rasteredImgParams.put("b64_encoded_image_data", encodedImage);
            }
            /* Encode response to Json */
            Gson gson = new Gson();
            return gson.toJson(rasteredImgParams);
        });

        /* Define the routing endpoint for HTTP GET requests. */
        get("/route", (req, res) -> {
            HashMap<String, Double> params =
                    getRequestParams(req, REQUIRED_ROUTE_REQUEST_PARAMS);
            LinkedList<Long> route = findAndSetRoute(params);
            return !route.isEmpty();
        });

        /* Define the API endpoint for clearing the firstent route. */
        get("/clear_route", (req, res) -> {
            clearRoute();
            return true;
        });

        /* Define the API endpoint for search */
        get("/search", (req, res) -> {
            Set<String> reqParams = req.queryParams();
            String term = req.queryParams("term");
            Gson gson = new Gson();
            /* Search for actual location data. */
            if (reqParams.contains("full")) {
                List<Map<String, Object>> data = getLocations(term);
                return gson.toJson(data);
            } else {
                /* Search for prefix matching strings. */
                List<String> matches = getLocationsByPrefix(term);
                return gson.toJson(matches);
            }
        });

        /* Define map application redirect */
        get("/", (request, response) -> {
            response.redirect("/map.html", 301);
            return true;
        });
    }

    /**
     * Validate & return a parameter map of the required request parameters.
     * Requires that all input parameters are doubles.
     *
     * @param req            HTTP Request
     * @param requiredParams TestParams to validate
     * @return A populated map of input parameter to it's numerical value.
     */
    private static HashMap<String, Double> getRequestParams(
            spark.Request req, String[] requiredParams) {
        Set<String> reqParams = req.queryParams();
        HashMap<String, Double> params = new HashMap<>();
        for (String param : requiredParams) {
            if (!reqParams.contains(param)) {
                halt(HALT_RESPONSE, "Request failed - parameters missing.");
            } else {
                try {
                    params.put(param, Double.parseDouble(req.queryParams(param)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    halt(HALT_RESPONSE, "Incorrect parameters - provide numbers.");
                }
            }
        }
        return params;
    }


    /**
     * Handles raster API calls, queries for tiles and rasters the full image. <br>
     * <p>
     * The rastered photo must have the following properties:
     * <ul>
     * <li>Has dimensions of at least w by h, where w and h are the user viewport width
     * and height.</li>
     * <li>The tiles collected must cover the most longitudinal distance per pixel
     * possible, while still covering less than or equal to the amount of
     * longitudinal distance per pixel in the query box for the user viewport size. </li>
     * <li>Contains all tiles that intersect the query bounding box that fulfill the
     * above condition.</li>
     * <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     * <li>If a firstent route exists, lines of width ROUTE_STROKE_WIDTH_PX and of color
     * ROUTE_STROKE_COLOR are drawn between all nodes on the route in the rastered photo.
     * </li>
     * </ul>
     * Additional image about the raster is returned and is to be included in the Json response.
     * </p>
     *
     * @param params Map of the HTTP GET request's query parameters - the query bounding box and
     *               the user viewport width and height.
     * @param os     An OutputStream that the resulting png image should be written to.
     * @return A map of parameters for the Json response as specified:
     * "raster_ul_lon" -> Double, the bounding upper left longitude of the rastered image <br>
     * "raster_ul_lat" -> Double, the bounding upper left latitude of the rastered image <br>
     * "raster_lr_lon" -> Double, the bounding lower right longitude of the rastered image <br>
     * "raster_lr_lat" -> Double, the bounding lower right latitude of the rastered image <br>
     * "raster_width"  -> Double, the width of the rastered image <br>
     * "raster_height" -> Double, the height of the rastered image <br>
     * "depth"         -> Double, the 1-indexed quadtree depth of the nodes of the rastered image.
     * Can also be interpreted as the length of the numbers in the image string. <br>
     * "query_success" -> Boolean, whether an image was successfully rastered. <br>
     * @see #REQUIRED_RASTER_REQUEST_PARAMS
     */
    public static Map<String, Object> getMapRaster(Map<String, Double> params, OutputStream os)
            throws IOException {

        double queryDpp = (params.get("lrlon") - params.get("ullon")) / params.get("w");
        int depth = qt.findDepth(qt.getRt(), queryDpp);

        Node queryNode = new Node("", params.get("ullon"), params.get("ullat"),
                params.get("lrlon"), params.get("lrlat"), depth);
        qt.clearTiles();

        ArrayList<Node> nodes = qt.getTiles(qt.getRt(), queryNode);
        Collections.sort(nodes);

        double rWidth = Math.abs(nodes.get(0).getUlon() - nodes.get(nodes.size() - 1).getLlon())
                / Math.abs(nodes.get(0).getUlon() - nodes.get(0).getLlon());

        double rHeight = Math.abs(nodes.get(0).getUlat() - nodes.get(nodes.size() - 1).getLlat())
                / Math.abs(nodes.get(0).getUlat() - nodes.get(0).getLlat());

        int roundedWidth = (int) (Math.round(rWidth * 256));
        int roundedHeight = (int) (Math.round(rHeight * 256));

        double lat = nodes.get(0).getUlat();
        double lon = nodes.get(0).getUlon();

        int h = 0;
        int w = 0;

        for (int j = 0; j < nodes.size(); j++) {
            if (nodes.get(j).getUlat() == lat) {
                w++;
            }
            if (nodes.get(j).getUlon() == lon) {
                h++;
            }
        }

        BufferedImage result = new BufferedImage((w * 256), (h * 256), BufferedImage.TYPE_INT_RGB);
        Graphics graphics = result.getGraphics();

        int x = 0;
        int y = 0;

        for (Node image : nodes) {
            String fileName = image.getId();
            BufferedImage bi = ImageIO.read(new File(IMG_ROOT + fileName + ".png"));
            if (x + bi.getWidth() > result.getWidth()) {
                x = 0;
                y += bi.getHeight();
            }
            graphics.drawImage(bi, x, y, null);
            x += bi.getWidth();
        }

        draw(nodes, result);

        ImageIO.write(result, "png", os);
        os.close();

        HashMap<String, Object> rasteredImageParams = new HashMap<>();

        rasteredImageParams.put("raster_ul_lon", nodes.get(0).getUlon());
        rasteredImageParams.put("raster_ul_lat", nodes.get(0).getUlat());
        rasteredImageParams.put("raster_lr_lon", nodes.get(nodes.size() - 1).getLlon());
        rasteredImageParams.put("raster_lr_lat", nodes.get(nodes.size() - 1).getLlat());
        rasteredImageParams.put("raster_width", roundedWidth);
        rasteredImageParams.put("raster_height", roundedHeight);
        rasteredImageParams.put("depth", nodes.get(0).getDepth());
        rasteredImageParams.put("query_success", true);

        return rasteredImageParams;
    }

    /*/**
     * Searches for the shortest route satisfying the input request parameters, sets it to be the
     * firstent route, and returns a <code>LinkedList</code> of the route's node ids for testing
     * purposes. <br>
     * The route should start from the closest node to the start point and end at the closest node
     * to the endpoint. Distance is defined as the euclidean between two points (lon1, lat1) and
     * (lon2, lat2).
     *
     * @param params from the API call described in REQUIRED_ROUTE_REQUEST_PARAMS
     * @param params from the API call described in REQUIRED_ROUTE_REQUEST_PARAMS
     * @return A LinkedList of node ids from the start of the route to the end.
     */

    private static void draw(ArrayList<Node> n, BufferedImage b) {
        ArrayList<GraphNode> transfer = new ArrayList<>(routeIDs.size());
        if (routeIDs != null && !routeIDs.isEmpty()) {

            for (long name : routeIDs) {
                transfer.add(g.gMap.get(name));
            }

            for (int i = 0; i < transfer.size() - 1; i++) {
                GraphNode first = transfer.get(i);
                GraphNode second = transfer.get(i + 1);

                int firstX = (int) Math.floor(
                        (Math.abs(first.getLon() - n.get(0).getUlon())
                                / Math.abs(n.get(n.size() - 1).getLlon()
                                - n.get(0).getUlon())) * b.getWidth());

                int firstY = (int) Math.floor(
                        (Math.abs((first.getLat() - n.get(0).getUlat())))
                                / Math.abs((n.get(n.size() - 1).getLlat())
                                - n.get(0).getUlat()) * b.getHeight());

                int secondX = (int) Math.floor(
                        (Math.abs((second.getLon() - n.get(0).getUlon()))
                                / Math.abs(n.get(n.size() - 1).getLlon()
                                - n.get(0).getUlon())) * b.getWidth());

                int secondY = (int) Math.floor(
                        (Math.abs((second.getLat() - n.get(0).getUlat()))
                                / Math.abs((n.get(n.size() - 1).getLlat()
                                - n.get(0).getUlat()))) * b.getHeight());

                Graphics2D g2 = (Graphics2D) b.getGraphics();
                g2.setColor(ROUTE_STROKE_COLOR);
                BasicStroke basic;
                basic = new BasicStroke(MapServer.ROUTE_STROKE_WIDTH_PX,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
                g2.setStroke(basic);
                g2.drawLine(firstX, firstY, secondX, secondY);
            }
        }
    }

    public static LinkedList<Long> findAndSetRoute(Map<String, Double> params) {
        // iterate through HashMap values and keep track of the euclidean distance
        // see which node is closest and use that node

        double startLon = params.get("start_lon");
        double startLat = params.get("start_lat");
        double endLon = params.get("end_lon");
        double endLat = params.get("end_lat");

        GraphNode startNode = null;
        double startOffset = Double.MAX_VALUE;

        GraphNode endNode = null;
        double endOffset = Double.MAX_VALUE;

        for (GraphNode gn : g.gMap.values()) {
            double startCompare = gn.calcDistance(gn.getLon(), gn.getLat(), startLon, startLat);
            double endCompare = gn.calcDistance(gn.getLon(), gn.getLat(), endLon, endLat);

            if (startCompare < startOffset) {
                startOffset = startCompare;
                startNode = gn;
            }
            if (endCompare < endOffset) {
                endOffset = endCompare;
                endNode = gn;
            }
        }

        if (endNode == null || startNode == null) {
            try {
                throw new IOException("Start and destination of route not found");
            } catch (IOException io) {
                System.out.println(io);
            }
        } else {
            Route r = new Route(startNode, endNode);
            routeIDs = r.path;
            for (GraphNode node : g.gMap.values()) {
                node.unmark();
            }
            for (long name : routeIDs) {
                GraphNode toAdd = g.gMap.get(name);
                routeNodes.addLast(toAdd);
            }
            return routeIDs;
        }
        return new LinkedList<>();
    }

    /**
     * Clear the firstent found route, if it exists.
     */
    public static void clearRoute() {
        routeIDs.clear();
        routeIDs = new LinkedList<>();
    }

    /**
     * In linear time, collect all the names of OSM locations that prefix-match the query string.
     *
     * @param prefix Prefix string to be searched for. Could be any case, with our without
     *               punctuation.
     * @return A <code>List</code> of the full names of locations whose cleaned name matches the
     * cleaned <code>prefix</code>.
     */
    public static List<String> getLocationsByPrefix(String prefix) {
        return new LinkedList<>();
    }

    /**
     * Collect all locations that match a cleaned <code>locationName</code>, and return
     * information about each node that matches.
     *
     * @param locationName A full name of a location searched for.
     * @return A list of locations whose cleaned name matches the
     * cleaned <code>locationName</code>, and each location is a map of parameters for the Json
     * response as specified: <br>
     * "lat" -> Number, The latitude of the node. <br>
     * "lon" -> Number, The longitude of the node. <br>
     * "name" -> String, The actual name of the node. <br>
     * "id" -> Number, The id of the node. <br>
     */
    public static List<Map<String, Object>> getLocations(String locationName) {
        return new LinkedList<>();
    }
}
