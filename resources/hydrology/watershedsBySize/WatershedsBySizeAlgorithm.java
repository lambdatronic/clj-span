package es.unex.sextante.hydrology.watershedsBySize;


import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.OutputObjectsSet;
import es.unex.sextante.core.ParametersSet;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.hydrology.accFlow.AccFlowAlgorithm;
import es.unex.sextante.hydrology.watersheds.WatershedsAlgorithm;
import es.unex.sextante.outputs.Output;

public class WatershedsBySizeAlgorithm
         extends
            GeoAlgorithm {

   public static final String WATERSHEDS   = "WATERSHEDS";
   public static final String SIZE         = "SIZE";
   public static final String DEM          = "DEM";

   private final static int   m_iOffsetX[] = { 0, 1, 1, 1, 0, -1, -1, -1 };
   private final static int   m_iOffsetY[] = { 1, 1, 0, -1, -1, -1, 0, 1 };

   private int                m_iNX, m_iNY;
   private double             m_dSize;

   private IRasterLayer       m_DEM        = null;
   private IRasterLayer       m_Outlets;
   private IRasterLayer       m_FlowAcc;


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("Watersheds_by_area"));
      setUserCanDefineAnalysisExtent(false);
      setGroup(Sextante.getText("Basic_hydrological_analysis"));

      try {
         m_Parameters.addInputRasterLayer(DEM, Sextante.getText("Elevation"), true);
         m_Parameters.addNumericalValue(SIZE, Sextante.getText("Tamano"), AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE,
                  10000., 0, Double.MAX_VALUE);

         addOutputRasterLayer(WATERSHEDS, Sextante.getText("Watersheds"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      m_DEM = m_Parameters.getParameterValueAsRasterLayer(DEM);
      m_DEM.setFullExtent();

      m_dSize = m_Parameters.getParameterValueAsDouble(SIZE);

      m_Outlets = getTempRasterLayer(IRasterLayer.RASTER_DATA_TYPE_INT, m_DEM.getWindowGridExtent());

      m_Outlets.setNoDataValue(0);
      m_Outlets.assignNoData();

      m_iNX = m_DEM.getNX();
      m_iNY = m_DEM.getNY();

      if (!calculateAccFlow()) {
         return false;
      }

      calculateOutlets();

      if (m_Task.isCanceled()) {
         return false;
      }
      else {
         return calculateWatersheds();
      }

   }


   private boolean calculateWatersheds() throws GeoAlgorithmExecutionException {

      try {
         setProgress(0, 100);
         setProgressText("3/3");
         final WatershedsAlgorithm alg = new WatershedsAlgorithm();
         final ParametersSet params = alg.getParameters();
         params.getParameter(WatershedsAlgorithm.DEM).setParameterValue(m_DEM);
         params.getParameter(WatershedsAlgorithm.NETWORK).setParameterValue(m_Outlets);

         final OutputObjectsSet oo = alg.getOutputObjects();
         Output output = oo.getOutput(WatershedsAlgorithm.WATERSHEDS);
         output.setOutputChannel(getOutputChannel(WATERSHEDS));

         final boolean bSucess = alg.execute(m_Task, m_OutputFactory);

         if (bSucess) {
            output = oo.getOutput(WatershedsAlgorithm.WATERSHEDS);
            m_OutputObjects.getOutput(WATERSHEDS).setOutputObject(output.getOutputObject());
            m_OutputObjects.getOutput(WATERSHEDS).setOutputChannel(output.getOutputChannel());
            return true;
         }
         else {
            return false;
         }


      }
      catch (final Exception e) {
         throw new GeoAlgorithmExecutionException(e.getMessage());
      }

   }


   private boolean calculateAccFlow() throws GeoAlgorithmExecutionException {

      try {
         setProgressText("1/3");
         final AccFlowAlgorithm alg = new AccFlowAlgorithm();
         final ParametersSet params = alg.getParameters();
         params.getParameter(AccFlowAlgorithm.METHOD).setParameterValue(AccFlowAlgorithm.D8);
         params.getParameter(AccFlowAlgorithm.DEM).setParameterValue(m_DEM);
         if (alg.execute(m_Task, m_OutputFactory)) {
            m_FlowAcc = (IRasterLayer) alg.getOutputObjects().getOutput(AccFlowAlgorithm.FLOWACC).getOutputObject();
            m_FlowAcc.open();
            m_FlowAcc.setFullExtent();
            m_DEM = m_Parameters.getParameterValueAsRasterLayer(DEM);
            m_DEM.open();
            m_DEM.setFullExtent();
            return true;
         }
         else {
            return false;
         }
      }
      catch (final Exception e) {
         throw new GeoAlgorithmExecutionException(e.getMessage());
      }

   }


   private void calculateOutlets() {

      double dValue;

      setProgressText("2/3");
      for (int x = 0; (x < m_iNX) && setProgress(x, m_iNX); x++) {
         for (int y = 0; y < m_iNY; y++) {
            dValue = m_FlowAcc.getCellValueAsDouble(x, y);
            if (!m_FlowAcc.isNoDataValue(dValue)) {
               setOutlet(x, y);
            }
         }
      }

   }


   private void setOutlet(final int x,
                          final int y) {

      int i;
      int ix, iy;
      int iDirection;
      int iValue, iValue2;
      double dValue;

      dValue = m_FlowAcc.getCellValueAsDouble(x, y);
      iValue = (int) (dValue / m_dSize);

      for (i = 0; i < 8; i++) {
         ix = x + m_iOffsetX[i];
         iy = y + m_iOffsetY[i];
         dValue = m_DEM.getCellValueAsDouble(ix, iy);
         if (!m_DEM.isNoDataValue(dValue)) {
            iDirection = m_DEM.getDirToNextDownslopeCell(ix, iy);
            if (iDirection >= 0) {
               if ((i + 4) % 8 == iDirection) {
                  iValue2 = (int) (m_FlowAcc.getCellValueAsDouble(ix, iy) / m_dSize);
                  if (iValue != iValue2) {
                     m_Outlets.setCellValue(ix, iy, -1);
                  }
               }
            }
         }
      }

   }

}
