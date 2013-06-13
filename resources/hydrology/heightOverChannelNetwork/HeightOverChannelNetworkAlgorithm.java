package es.unex.sextante.hydrology.heightOverChannelNetwork;

import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.OutputObjectsSet;
import es.unex.sextante.core.ParametersSet;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.gridTools.closeGaps.CloseGapsAlgorithm;
import es.unex.sextante.rasterWrappers.GridWrapper;

public class HeightOverChannelNetworkAlgorithm
         extends
            GeoAlgorithm {

   public static final String DEM        = "DEM";
   public static final String NETWORK    = "NETWORK";
   public static final String HEIGHTOVER = "HEIGHTOVER";
   public static final String THRESHOLD  = "THRESHOLD";

   private int                m_iNX, m_iNY;

   private IRasterLayer       m_DEM      = null;
   private IRasterLayer       m_Network  = null;
   private IRasterLayer       m_Diff;
   private double             m_dThreshold;


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      m_DEM = m_Parameters.getParameterValueAsRasterLayer(DEM);
      m_Network = m_Parameters.getParameterValueAsRasterLayer(NETWORK);
      m_dThreshold = m_Parameters.getParameterValueAsDouble(THRESHOLD);

      m_Diff = getNewRasterLayer(HEIGHTOVER, Sextante.getText("Elevation_over_channel_network"),
               IRasterLayer.RASTER_DATA_TYPE_FLOAT);

      m_Diff.setNoDataValue(-1.0);
      //m_Diff.assignNoData();

      final AnalysisExtent extent = m_Diff.getWindowGridExtent();
      m_DEM.setWindowExtent(extent);
      m_Network.setWindowExtent(extent);
      m_Network.setInterpolationMethod(GridWrapper.INTERPOLATION_NearestNeighbour);

      m_iNX = m_DEM.getNX();
      m_iNY = m_DEM.getNY();

      return interpolateSurfaceAndSubstract();

   }


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("Elevation_over_channel_network"));
      setGroup(Sextante.getText("Indices_and_other_hydrological_parameters"));
      setUserCanDefineAnalysisExtent(true);

      try {
         m_Parameters.addInputRasterLayer(DEM, Sextante.getText("Elevation"), true);
         m_Parameters.addInputRasterLayer(NETWORK, Sextante.getText("Channel_network"), true);
         m_Parameters.addNumericalValue(THRESHOLD, Sextante.getText("Tension_threshold"), 0.1,
                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
         addOutputRasterLayer(HEIGHTOVER, Sextante.getText("Elevation_over_channel_network"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   private boolean interpolateSurfaceAndSubstract() throws GeoAlgorithmExecutionException {

      int x, y;
      double dValue, dValue2;

      for (y = 0; y < m_iNY; y++) {
         for (x = 0; x < m_iNX; x++) {
            dValue = m_Network.getCellValueAsDouble(x, y);
            if ((dValue != 0) && !m_Network.isNoDataValue(dValue)) {
               dValue = m_DEM.getCellValueAsDouble(x, y);
               m_Diff.setCellValue(x, y, dValue);
            }
            else {
               m_Diff.setNoData(x, y);
            }
         }
      }

      final CloseGapsAlgorithm alg = new CloseGapsAlgorithm();
      final ParametersSet ps = alg.getParameters();

      //IRasterLayer layer = m_Diff.getRasterLayer(" ",  getAlgorithmProjection());
      ps.getParameter(CloseGapsAlgorithm.INPUT).setParameterValue(m_Diff);
      ps.getParameter(CloseGapsAlgorithm.THRESHOLD).setParameterValue(new Double(m_dThreshold));
      if (alg.execute(this.m_Task, this.m_OutputFactory)) {
         final OutputObjectsSet output = alg.getOutputObjects();
         final IRasterLayer networkHeight = (IRasterLayer) output.getOutput("RESULT").getOutputObject();
         networkHeight.open();
         for (y = 0; (y < m_iNY) && setProgress(y, m_iNY); y++) {
            for (x = 0; x < m_iNX; x++) {
               dValue = m_DEM.getCellValueAsDouble(x, y);
               dValue2 = networkHeight.getCellValueAsDouble(x, y);
               if (m_DEM.isNoDataValue(dValue) || networkHeight.isNoDataValue(dValue2)) {
                  m_Diff.setNoData(x, y);
               }
               else {
                  m_Diff.setCellValue(x, y, dValue - dValue2);
               }
            }
         }
         networkHeight.close();
         return !m_Task.isCanceled();
      }
      else {
         return false;
      }

   }

}
