package es.unex.sextante.hydrology.hydroModel;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;

import es.unex.sextante.additionalInfo.AdditionalInfoMultipleInput;
import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.additionalInfo.AdditionalInfoVectorLayer;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IFeature;
import es.unex.sextante.dataObjects.IFeatureIterator;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.dataObjects.IRecord;
import es.unex.sextante.dataObjects.IRecordsetIterator;
import es.unex.sextante.dataObjects.ITable;
import es.unex.sextante.dataObjects.IVectorLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.OptionalParentParameterException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.exceptions.UndefinedParentParameterNameException;
import es.unex.sextante.exceptions.UnsupportedOutputChannelException;
import es.unex.sextante.hydrology.modelling.Hydrograph;
import es.unex.sextante.hydrology.modelling.Hyetograph;

public class HydroModelAlgorithm
         extends
            GeoAlgorithm {

   public static final String TABLES               = "TABLES";
   public static final String TIMEOUT              = "TIMEOUT";
   public static final String CN                   = "CN";
   public static final String INTERVALHYDRO        = "INTERVALHYDRO";
   public static final String INTERVALHYETO        = "INTERVALHYETO";
   public static final String FIELD                = "FIELD";
   public static final String STATIONS             = "STATIONS";

   private int                m_iNX, m_iNY;
   private int                m_iIntervalHydro;
   private int                m_iIntervalHyeto;
   private IRasterLayer       m_TimeOut            = null;
   private IRasterLayer       m_CN                 = null;
   private ArrayList          m_Hydrographs;
   private Hyetograph         m_Hyetogram[][];
   private ArrayList          m_Tables;
   private int                m_iTableNameField;
   private boolean            m_bHyetoNamesCreated = false;
   private String[]           m_sHyetoNames;


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {


      m_TimeOut = m_Parameters.getParameterValueAsRasterLayer(TIMEOUT);
      m_CN = m_Parameters.getParameterValueAsRasterLayer(CN);
      m_iIntervalHydro = m_Parameters.getParameterValueAsInt(INTERVALHYDRO) * 60;
      m_iIntervalHyeto = m_Parameters.getParameterValueAsInt(INTERVALHYETO) * 60;
      m_iTableNameField = m_Parameters.getParameterValueAsInt(FIELD);
      m_TimeOut.setFullExtent();
      m_CN.setWindowExtent(m_TimeOut.getWindowGridExtent());

      m_iNX = m_TimeOut.getNX();
      m_iNY = m_TimeOut.getNY();

      if (createHyetographs()) {
         calculateHydrographs();
         documentHydrographs();
      }
      else {
         throw new GeoAlgorithmExecutionException(Sextante.getText("Hyetographs_are_not_consistent"));
      }

      return !m_Task.isCanceled();

   }


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("A_simple_hydrological_model"));
      setGroup(Sextante.getText("Indices_and_other_hydrological_parameters"));
      setUserCanDefineAnalysisExtent(false);

      try {
         m_Parameters.addInputRasterLayer(TIMEOUT, Sextante.getText("Time_to_outlet"), true);
         m_Parameters.addInputRasterLayer(CN, Sextante.getText("N\u00famero_de_Curva"), true);
         m_Parameters.addInputVectorLayer(STATIONS, Sextante.getText("Estaciones"), AdditionalInfoVectorLayer.SHAPE_TYPE_POINT,
                  true);
         m_Parameters.addTableField(FIELD, Sextante.getText("Nombre_de_tabla_con_hietograma"), STATIONS);
         m_Parameters.addMultipleInput(TABLES, Sextante.getText("Tables"), AdditionalInfoMultipleInput.DATA_TYPE_TABLE, true);
         m_Parameters.addNumericalValue(INTERVALHYDRO, Sextante.getText("Intervalo_en_hidrogramas__minutos"),
                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 10, 1, Integer.MAX_VALUE);
         m_Parameters.addNumericalValue(INTERVALHYETO, Sextante.getText("Intervalo_en_hietogramas__minutos"),
                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 30, 1, Integer.MAX_VALUE);
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }
      catch (final UndefinedParentParameterNameException e) {
         Sextante.addErrorToLog(e);
      }
      catch (final OptionalParentParameterException e) {
         Sextante.addErrorToLog(e);
      }

   }


   private boolean createHyetographs() {

      int iPoint = 0;

      try {
         final IVectorLayer stations = m_Parameters.getParameterValueAsVectorLayer(STATIONS);
         m_Hyetogram = new Hyetograph[stations.getShapesCount()][];
         final IFeatureIterator iter = stations.iterator();
         while (iter.hasNext()) {
            final IFeature feature = iter.next();
            final String sTableName = feature.getRecord().getValue(m_iTableNameField).toString();
            final Coordinate c = feature.getGeometry().getCoordinate();
            m_Hyetogram[iPoint] = getHyetogramsFromTable(c.x, c.y, sTableName);
            iPoint++;
         }
         iter.close();

         return checkHyetogramsConsistency();
      }
      catch (final Exception e) {
         Sextante.addErrorToLog(e);
         return false;
      }

   }


   private boolean checkHyetogramsConsistency() {

      try {
         final int iIntervals = m_Hyetogram[0][0].getIntervals();
         for (int i = 0; i < m_Hyetogram.length; i++) {
            if (m_Hyetogram[i].length != m_Hyetogram[0].length) {
               return false;
            }
            if (iIntervals != m_Hyetogram[i][0].getIntervals()) {
               return false;
            }
            for (final Hyetograph[] element : m_Hyetogram) {
               if (element[i] == null) {
                  return false;
               }
            }
         }
         return true;
      }
      catch (final Exception e) {
         return false;
      }

   }


   private Hyetograph[] getHyetogramsFromTable(final double x,
                                               final double y,
                                               final String sName) {

      try {
         final ITable table = getTableFromName(sName);
         if (table != null) {
            final int iCount = table.getFieldCount();

            if (iCount <= 0) {
               return null;
            }

            if (!m_bHyetoNamesCreated) {
               m_bHyetoNamesCreated = true;
               m_sHyetoNames = new String[iCount];
               for (int i = 0; i < iCount; i++) {
                  m_sHyetoNames[i] = table.getFieldName(i);
               }
            }

            final Hyetograph hyetos[] = new Hyetograph[iCount];
            for (int i = 0; i < iCount; i++) {
               final double dHyeto[] = new double[(int) table.getRecordCount()];
               final IRecordsetIterator iter = table.iterator();
               int j = 0;
               while (iter.hasNext()) {
                  final IRecord record = iter.next();
                  String s = record.getValue(i).toString();
                  s = s.replaceAll("'", "");
                  try {
                     dHyeto[j] = Double.parseDouble(s);
                  }
                  catch (final NumberFormatException nfe) {
                     dHyeto[j] = 0;
                  }
                  j++;
               }
               hyetos[i] = new Hyetograph(dHyeto, this.m_iIntervalHyeto);
               hyetos[i].setName(sName + table.getFieldName(i));
               hyetos[i].setCoords(new Point2D.Double(x, y));
            }

            return hyetos;
         }
         else {
            return null;
         }
      }
      catch (final Exception e) {
         return null;
      }

   }


   private ITable getTableFromName(final String sName) {

      for (int i = 0; i < m_Tables.size(); i++) {
         final ITable table = (ITable) m_Tables.get(i);
         if (table.getName().equals(sName)) {
            return table;
         }
      }

      return null;

   }


   private void calculateHydrographs() throws UnsupportedOutputChannelException {

      int i, j, k;
      int x, y;
      int iLength;
      int iRatio;
      int iIndex;
      int iIntervalsHyeto, iIntervalsHydro;
      double hydData[];
      double dRain;
      double dTimeOut;
      double dRunoff;
      double dAccRunoff;
      final double dRunoff2Volume = m_TimeOut.getWindowCellSize() * m_TimeOut.getWindowCellSize() / 1000.0;
      IRasterLayer swap;
      IRasterLayer accRainGrid;
      IRasterLayer accRunoffGrid;
      IRasterLayer runoffGrid;

      m_Hydrographs = new ArrayList();

      final AnalysisExtent extent = m_TimeOut.getWindowGridExtent();

      accRainGrid = getTempRasterLayer(IRasterLayer.RASTER_DATA_TYPE_FLOAT, extent);
      accRunoffGrid = getTempRasterLayer(IRasterLayer.RASTER_DATA_TYPE_FLOAT, extent);
      runoffGrid = getTempRasterLayer(IRasterLayer.RASTER_DATA_TYPE_FLOAT, extent);

      iLength = m_Hyetogram[0][0].getDuration();
      iIntervalsHyeto = m_Hyetogram[0][0].getIntervals();
      iRatio = m_iIntervalHyeto / m_iIntervalHydro;
      iIntervalsHydro = (int) ((iLength + m_TimeOut.getMaxValue() * 3600) / m_iIntervalHydro) + 1;


      for (i = 0; i < m_Hyetogram[0].length; i++) {//for each return period

         accRainGrid.assign(0.0);
         accRunoffGrid.assign(0.0);
         runoffGrid.assign(0.0);

         hydData = new double[iIntervalsHydro];
         for (j = 0; j < iIntervalsHydro; j++) {
            hydData[j] = 0;
         }

         this.setProgressText(Sextante.getText("Calculando_hidrograma") + "(" + m_sHyetoNames[i] + ")");
         for (j = 0; j < iIntervalsHyeto; j++) {
            for (y = 0; y < m_iNY; y++) {
               for (x = 0; x < m_iNX; x++) {
                  dTimeOut = m_TimeOut.getCellValueAsDouble(x, y);
                  if (!m_TimeOut.isNoDataValue(dTimeOut) && (dTimeOut > 0)) {
                     dRain = getRainfall(i, extent.getWorldCoordsFromGridCoords(x, y), j * m_iIntervalHyeto, m_iIntervalHyeto);
                     if (dRain > 0) {
                        accRainGrid.addToCellValue(x, y, dRain);
                     }
                  }
               }
            }

            calculateRunoffGrid(runoffGrid, accRainGrid);

            for (y = 1; y < m_iNY; y++) {
               for (x = 0; x < m_iNX; x++) {
                  dTimeOut = m_TimeOut.getCellValueAsDouble(x, y);
                  if (!m_TimeOut.isNoDataValue(dTimeOut) && (dTimeOut > 0)) {
                     iIndex = (int) ((dTimeOut * 3600 + j * m_iIntervalHyeto) / m_iIntervalHydro);
                     dRunoff = runoffGrid.getCellValueAsDouble(x, y);
                     dAccRunoff = accRunoffGrid.getCellValueAsDouble(x, y);
                     dRunoff -= dAccRunoff;
                     if (dRunoff > 0) {
                        for (k = 0; k < iRatio; k++) {
                           hydData[iIndex] += dRunoff;
                        }
                     }
                  }
               }
            }

            swap = runoffGrid;
            runoffGrid = accRunoffGrid;
            accRunoffGrid = swap;

            setProgress(j, iIntervalsHyeto);

         }

         for (j = 0; j < iIntervalsHydro; j++) {
            hydData[j] = hydData[j] * dRunoff2Volume / iRatio / m_iIntervalHydro;
         }

         final Hydrograph hyd = new Hydrograph(hydData, m_iIntervalHydro);
         hyd.setName(m_Hyetogram[0][i].getName());
         m_Hydrographs.add(hyd);

      }

   }


   private double getRainfall(final int iSerie,
                              final Point2D pt,
                              final int iInitTime,
                              final int iInterval) {

      double dTotalRainfall = 0;
      double dTotalWeight = 0;

      for (final Hyetograph[] element : m_Hyetogram) {
         final Point2D coords = element[iSerie].getCoords();
         final double dWeight = 1. / Math.pow(coords.distance(pt), 2.);
         final double dRainfall = element[iSerie].getRainfall(iInitTime, iInterval) * dWeight;
         dTotalRainfall += dRainfall;
         dTotalWeight += dWeight;
      }

      return dTotalRainfall / dTotalWeight;

   }


   private void documentHydrographs() throws UnsupportedOutputChannelException {

      int i, j;
      double flow[];
      Hydrograph hydro;
      String sTableDescription;
      String sTableName;
      final String sFields[] = { "T", "Q" };
      final Object[] values = new Object[2];
      final Class[] types = { Integer.class, Double.class };
      ITable driver;

      for (i = 0; i < m_Hydrographs.size(); i++) {
         hydro = (Hydrograph) m_Hydrographs.get(i);
         sTableName = "HYDROGRAPH" + Integer.toString(i);
         sTableDescription = "Hidrograma (" + m_sHyetoNames[i] + ")";
         driver = getNewTable(sTableName, sTableDescription, types, sFields);
         flow = hydro.getFlowArray();
         for (j = 0; j < hydro.getLengthInIntervals(); j++) {
            values[0] = new Integer(j * hydro.getTimeInterval());
            values[1] = new Double(flow[j]);
            driver.addRecord(values);
         }
         setProgress(i, m_Hydrographs.size());
      }

   }


   private double getRunoff(final double dRainfall, //in mm
                            final double dCN) {

      double dS;
      double dRunoff;

      dS = (25400.0 / dCN) - 254.0;

      if (dRainfall < (0.2 * dS)) {
         return 0;
      }

      dRunoff = Math.pow(dRainfall - 0.2 * dS, 2.0) / (dRainfall + 0.8 * dS);

      return dRunoff;

   }


   private void calculateRunoffGrid(final IRasterLayer runoff,
                                    final IRasterLayer rainfall) {

      int x, y;
      double dCN;
      double dRunoff;
      double dRainfall;

      double dMean = 0;
      int i = 0;

      for (y = 0; y < m_iNY; y++) {
         for (x = 0; x < m_iNX; x++) {
            dCN = m_CN.getCellValueAsDouble(x, y);
            dRainfall = rainfall.getCellValueAsDouble(x, y);
            if (!m_CN.isNoDataValue(dCN) && (dRainfall != 0.0)) {
               i++;
               dRunoff = getRunoff(dRainfall, dCN);
               runoff.setCellValue(x, y, dRunoff);
               dMean += dRunoff;
            }
         }
      }
      dMean /= (i);

      dMean += 1;

   }


   @Override
   public boolean isSuitableForModelling() {

      return false;

   }


}
