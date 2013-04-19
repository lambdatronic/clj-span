package es.unex.sextante.hydrology.cFactorFromNDVI;

import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;

public class CFactorFromNDVIAlgorithm
         extends
            GeoAlgorithm {

   public static final String ALPHA   = "ALPHA";
   public static final String BETA    = "BETA";
   public static final String NDVI    = "NDVI";
   public static final String CFACTOR = "CFACTOR";

   private IRasterLayer       m_NDVI  = null;
   private IRasterLayer       m_CFactor;


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      int x, y;
      int iNX, iNY;
      double dNDVI;
      double dAlpha, dBeta;

      m_NDVI = m_Parameters.getParameterValueAsRasterLayer(NDVI);
      m_CFactor = getNewRasterLayer(CFACTOR, "C", IRasterLayer.RASTER_DATA_TYPE_FLOAT);
      dAlpha = m_Parameters.getParameterValueAsDouble(ALPHA);
      dBeta = m_Parameters.getParameterValueAsDouble(BETA);

      m_NDVI.setWindowExtent(m_AnalysisExtent);

      iNX = m_NDVI.getNX();
      iNY = m_NDVI.getNY();

      for (y = 0; (y < iNY) && setProgress(y, iNY); y++) {
         for (x = 0; x < iNX; x++) {
            dNDVI = m_NDVI.getCellValueAsDouble(x, y);
            if (m_NDVI.isNoDataValue(dNDVI)) {
               m_CFactor.setNoData(x, y);
            }
            else {
               m_CFactor.setCellValue(x, y, Math.exp(-dAlpha * dNDVI / (dBeta - dNDVI)));
            }
         }
      }


      return !m_Task.isCanceled();
   }


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("C_factor_from_NDVI"));
      setGroup(Sextante.getText("Indices_and_other_hydrological_parameters"));
      setUserCanDefineAnalysisExtent(true);

      try {
         m_Parameters.addInputRasterLayer(NDVI, "NDVI", true);
         m_Parameters.addNumericalValue(ALPHA, "Alpha", 1., AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
         m_Parameters.addNumericalValue(BETA, "Beta", 2., AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
         addOutputRasterLayer(CFACTOR, Sextante.getText("C"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }

}
