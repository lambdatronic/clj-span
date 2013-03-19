import clj_span.java_span_bridge;
import java.util.HashMap;

public class APITest {

    public static void main(String[] args) {

        int rows = 10;
        int cols = 10;

        double[] sourceLayer =
            {0.0,100.0,0.0,0.0,0.0,100.0,0.0,  0.0,  0.0,0.0,
             0.0,  0.0,0.0,0.0,0.0,  0.0,0.0,  0.0,  0.0,0.0,
             0.0,  0.0,0.0,0.0,0.0,  0.0,0.0,  0.0,  0.0,0.0,
             0.0,  0.0,0.0,0.0,0.0,  0.0,0.0,100.0,  0.0,0.0,
             0.0,  0.0,0.0,0.0,0.0,  0.0,0.0,  0.0,  0.0,0.0,
             0.0,  0.0,0.0,0.0,0.0,  0.0,0.0,  0.0,  0.0,0.0,
             0.0,  0.0,0.0,0.0,0.0,  0.0,0.0,  0.0,  0.0,0.0,
             0.0,  0.0,0.0,0.0,0.0,  0.0,0.0,  0.0,  0.0,0.0,
             0.0,  0.0,0.0,0.0,0.0,  0.0,0.0,  0.0,100.0,0.0,
             0.0,  0.0,0.0,0.0,0.0,  0.0,0.0,  0.0,  0.0,0.0};

        double[] sinkLayer =
            {0.0,0.0,0.0,0.0,0.0, 0.0,  0.0, 0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0, 0.0,  0.0, 0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0, 0.0,  0.0, 0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,10.0,  0.0, 0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0, 0.0,  0.0, 0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,10.0,  0.0, 0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0, 0.0,  0.0, 0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0, 0.0,  0.0, 0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0, 0.0,100.0,10.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0, 0.0,  0.0, 0.0,0.0,0.0};

        double[] useLayer =
            {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};

        double[] elevLayer = 
            {30.0,60.0,32.0,32.0,32.0,28.0,11.0, 5.0, 5.0,5.0,
             30.0,29.0,27.0,27.0,27.0,20.0, 6.0, 5.0, 5.0,5.0,
             30.0,28.0,22.0,22.0,22.0,15.0, 3.0, 5.0, 5.0,5.0,
             30.0,27.0,17.0,17.0,17.0,11.0, 2.0, 2.0, 5.0,5.0,
             30.0,26.0,12.0, 8.0, 9.0, 9.0, 0.0, 1.0, 5.0,5.0,
             30.0,25.0, 7.0, 3.0, 5.0, 5.0, 1.0, 3.0, 5.0,5.0,
             30.0,24.0, 2.0, 2.0, 4.0, 4.0, 3.0, 5.0, 8.0,5.0,
             30.0,23.0, 1.0, 3.0, 3.0, 3.0, 8.0, 9.0,11.0,5.0,
             30.0,22.0, 1.0, 3.0, 7.0, 9.0,12.0,13.0,20.0,5.0,
             30.0,21.0, 1.0, 3.0, 8.0, 9.0,14.0,15.0,17.0,5.0};

        double[] waterLayer =
            {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,
             0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};

        HashMap<String,double[]> flowLayers = new HashMap<String,double[]>();
        flowLayers.put("Altitude", elevLayer);
        flowLayers.put("WaterBodies", waterLayer);

        double sourceThreshold = 0.0;
        double sinkThreshold = 0.0;
        double useThreshold = 0.0;
        double transThreshold = 1.0;

        double cellWidth = 100.0;
        double cellHeight = 100.0;

        int rvMaxStates = 10;

        double downscalingFactor = 1.0;

        String sourceType = "infinite";
        String sinkType = "infinite";
        String useType = "infinite";

        String benefitType = "non-rival";

        String valueType = "numbers";

        String flowModel = "LineOfSight";

        boolean animation = false;

        String[] resultLayers = {"theoretical-source","actual-sink","possible-use","blocked-flow"};

        HashMap<String,Object> spanParams = new HashMap<String,Object>();
        spanParams.put("source-layer", sourceLayer);
        spanParams.put("sink-layer", sinkLayer);
        spanParams.put("use-layer", useLayer);
        spanParams.put("flow-layers", flowLayers);
        spanParams.put("rows", rows);
        spanParams.put("cols", cols);
        spanParams.put("source-threshold", sourceThreshold);
        spanParams.put("sink-threshold", sinkThreshold);
        spanParams.put("use-threshold", useThreshold);
        spanParams.put("trans-threshold", transThreshold);
        spanParams.put("cell-width", cellWidth);
        spanParams.put("cell-height", cellHeight);
        spanParams.put("rv-max-states", rvMaxStates);
        spanParams.put("downscaling-factor", downscalingFactor);
        spanParams.put("source-type", sourceType);
        spanParams.put("sink-type", sinkType);
        spanParams.put("use-type", useType);
        spanParams.put("benefit-type", benefitType);
        spanParams.put("value-type", valueType);
        spanParams.put("flow-model", flowModel);
        spanParams.put("animation?", animation);
        spanParams.put("result-layers", resultLayers);

        HashMap<String,Object> resultMap = clj_span.java_span_bridge.runSpan(spanParams);

        System.out.println(resultMap);
    }
}
