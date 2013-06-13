package es.unex.sextante.hydrology.createHyetogram;

import java.util.ArrayList;
import java.util.StringTokenizer;

import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.ITable;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.exceptions.UnsupportedOutputChannelException;
import es.unex.sextante.hydrology.modelling.Hyetograph;

public class CreateHyetogramAlgorithm
         extends
            GeoAlgorithm {

   public static final String RETURNS          = "RETURNS";
   public static final String HYETOGRAPHLENGTH = "HYETOGRAPHLENGTH";
   public static final String TIMETOPEAK       = "TIMETOPEAK";
   public static final String INTERVAL         = "INTERVAL";
   public static final String AVERAGE          = "AVERAGE";
   public static final String STDDEV           = "STDDEV";
   public static final String HYETOGRAPH       = "HYETOGRAPH";

   private int                m_iInterval;
   private int                m_iPeakTime;
   private int                m_iHyetoLength;
   private final int          m_iRatioI1       = 10;                //TODO cambiar esto
   private ArrayList          m_Returns;
   private double             m_dMean;
   private double             m_dStdDev;


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      m_iInterval = m_Parameters.getParameterValueAsInt(INTERVAL) * 60;
      m_iHyetoLength = m_Parameters.getParameterValueAsInt(HYETOGRAPHLENGTH) * 60;
      m_iPeakTime = m_Parameters.getParameterValueAsInt(TIMETOPEAK) * 60;
      m_dMean = m_Parameters.getParameterValueAsDouble(AVERAGE);
      m_dStdDev = m_Parameters.getParameterValueAsDouble(STDDEV);

      m_Returns = new ArrayList();
      final String sReturns = m_Parameters.getParameterValueAsString(RETURNS);
      final StringTokenizer st = new StringTokenizer(sReturns);
      while (st.hasMoreTokens()) {
         try {
            final String token = st.nextToken();
            final Integer value = Integer.valueOf(token);
            m_Returns.add(value);
         }
         catch (final NumberFormatException e) {}
      }

      createTable();

      return true;
   }


   private void createTable() throws UnsupportedOutputChannelException {

      int i, j;
      int iReturn;
      final String sFields[] = new String[m_Returns.size() + 1];
      final Object values[] = new Object[m_Returns.size() + 1];
      final Class types[] = new Class[m_Returns.size() + 1];
      final Hyetograph hyetos[] = new Hyetograph[m_Returns.size()];
      ITable driver;

      types[0] = Double.class;
      sFields[0] = "T";
      for (i = 0; i < m_Returns.size(); i++) {
         types[i + 1] = Double.class;
         sFields[i + 1] = "P_" + ((Integer) m_Returns.get(i)).toString();
      }

      driver = getNewTable(HYETOGRAPH, Sextante.getText("Hietograma"), types, sFields);

      for (i = 0; i < m_Returns.size(); i++) {
         iReturn = ((Integer) m_Returns.get(i)).intValue();
         hyetos[i] = getHyetograph(iReturn);
      }

      for (j = 0; j < hyetos[0].getIntervals(); j++) {
         values[0] = new Double(j * hyetos[0].getTimeInterval());
         for (int k = 0; k < hyetos.length; k++) {
            values[k + 1] = new Double(hyetos[k].getRainfallAtInterval(j));
         }
         driver.addRecord(values);
      }

   }


   private Hyetograph getHyetograph(final int iReturn) {

      final double dAlpha = Math.sqrt(Math.pow(3.141592, 2.0) / 6.0 / m_dStdDev);
      final double dMu = m_dMean - (0.5772 / dAlpha);

      final double dFx = 1.0 - (1.0 / iReturn);
      double dP24 = (float) -Math.log(Math.log(1.0 / dFx));
      dP24 = dP24 / dAlpha + dMu;

      return new Hyetograph(dP24, m_iHyetoLength, m_iInterval, m_iPeakTime, iReturn, m_iRatioI1);

   }


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("Create_synthetic_hyetograph"));
      setGroup(Sextante.getText("Indices_and_other_hydrological_parameters"));
      setUserCanDefineAnalysisExtent(false);

      try {
         m_Parameters.addString(RETURNS, Sextante.getText("Return_periods"), "5 10 25 50 100");
         m_Parameters.addNumericalValue(HYETOGRAPHLENGTH, Sextante.getText("Total_length__minutes"),
                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 180, Integer.MIN_VALUE, Integer.MAX_VALUE);
         m_Parameters.addNumericalValue(TIMETOPEAK, Sextante.getText("Time_to_peak__minutes"),
                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 120, Integer.MIN_VALUE, Integer.MAX_VALUE);
         m_Parameters.addNumericalValue(INTERVAL, Sextante.getText("Interval__minutes"),
                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 10, 1, Integer.MAX_VALUE);
         m_Parameters.addNumericalValue(AVERAGE, Sextante.getText("Rainfall_mean"),
                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE, 0, 0, Double.MAX_VALUE);
         m_Parameters.addNumericalValue(STDDEV, Sextante.getText("Standard_deviation"),
                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE, 0, 0, Double.MAX_VALUE);
         addOutputTable(HYETOGRAPH, Sextante.getText("Hietograma"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }

}
