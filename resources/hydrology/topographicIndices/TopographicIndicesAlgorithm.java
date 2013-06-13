package es.unex.sextante.hydrology.topographicIndices;

import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;

public class TopographicIndicesAlgorithm
         extends
            GeoAlgorithm {

   private static final double ALMOST_ZERO = 0.0011;

   public static final String  SLOPE       = "SLOPE";
   public static final String  ACCFLOW     = "ACCFLOW";
   public static final String  TWI         = "TWI";
   public static final String  SPI         = "SPI";
   public static final String  LS          = "LS";

   private IRasterLayer        m_Slope     = null;
   private IRasterLayer        m_AccFlow   = null;
   private IRasterLayer        m_WetnessIndex;
   private IRasterLayer        m_StreamPowerIndex;
   private IRasterLayer        m_LSFactor;


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      int x, y;
      int iNX, iNY;

      m_AccFlow = m_Parameters.getParameterValueAsRasterLayer(ACCFLOW);
      m_Slope = m_Parameters.getParameterValueAsRasterLayer(SLOPE);

      m_WetnessIndex = getNewRasterLayer(TWI, Sextante.getText("Topographic_Wetness_Index__TWI"),
               IRasterLayer.RASTER_DATA_TYPE_FLOAT);
      m_StreamPowerIndex = getNewRasterLayer(SPI, Sextante.getText("Stream_Power_Index"), IRasterLayer.RASTER_DATA_TYPE_FLOAT);
      m_LSFactor = getNewRasterLayer(LS, Sextante.getText("LS_factor"), IRasterLayer.RASTER_DATA_TYPE_FLOAT);

      final AnalysisExtent extent = m_WetnessIndex.getWindowGridExtent();
      m_Slope.setWindowExtent(extent);
      m_AccFlow.setWindowExtent(extent);

      iNX = m_Slope.getNX();
      iNY = m_Slope.getNY();

      for (y = 0; (y < iNY) && setProgress(y, iNY); y++) {
         for (x = 0; x < iNX; x++) {
            calculateIndices(x, y);
         }
      }

      return !m_Task.isCanceled();
   }


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("Topographic_indices"));
      setGroup(Sextante.getText("Indices_and_other_hydrological_parameters"));
      setUserCanDefineAnalysisExtent(true);

      try {
         m_Parameters.addInputRasterLayer(SLOPE, Sextante.getText("Slope"), true);
         m_Parameters.addInputRasterLayer(ACCFLOW, Sextante.getText("Flow_accumulation"), true);
         addOutputRasterLayer(TWI, Sextante.getText("Topographic_Wetness_Index__TWI"));
         addOutputRasterLayer(SPI, Sextante.getText("Stream_Power_Index"));
         addOutputRasterLayer(LS, Sextante.getText("LS_factor"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   private void calculateIndices(final int x,
                                 final int y) {

      double dSlope = m_Slope.getCellValueAsDouble(x, y);
      double dAccFlow = m_AccFlow.getCellValueAsDouble(x, y);

      if (m_Slope.isNoDataValue(dSlope) || m_AccFlow.isNoDataValue(dAccFlow)) {
         m_WetnessIndex.setNoData(x, y);
         m_StreamPowerIndex.setNoData(x, y);
         m_LSFactor.setNoData(x, y);
      }
      else {
         dAccFlow /= m_AccFlow.getWindowCellSize();
         dSlope = Math.max(Math.tan(dSlope), ALMOST_ZERO);
         m_WetnessIndex.setCellValue(x, y, Math.log(dAccFlow / dSlope));
         m_StreamPowerIndex.setCellValue(x, y, dAccFlow * dSlope);
         m_LSFactor.setCellValue(x, y, (0.4 + 1) * Math.pow(dAccFlow / 22.13, 0.4) * Math.pow(Math.sin(dSlope) / 0.0896, 1.3));
      }

   }

}
