package es.unex.sextante.hydrology.slopeLength;

import java.util.Arrays;

import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.rasterWrappers.GridCell;

public class SlopeLengthAlgorithm
         extends
            GeoAlgorithm {

   private final static int   m_iOffsetX[] = { 0, 1, 1, 1, 0, -1, -1, -1 };
   private final static int   m_iOffsetY[] = { 1, 1, 0, -1, -1, -1, 0, 1 };

   public static final String DEM          = "DEM";
   public static final String USETHRESHOLD = "USETHRESHOLD";
   public static final String THRESHOLD    = "THRESHOLD";
   public static final String SLOPELENGTH  = "SLOPELENGTH";

   private int                m_iNX, m_iNY;

   private IRasterLayer       m_DEM        = null;
   private IRasterLayer       m_SlopeLength;
   private IRasterLayer       m_Slope;
   private double             m_dThreshold;
   private boolean            m_bUseThreshold;


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      int i;

      m_DEM = m_Parameters.getParameterValueAsRasterLayer(DEM);
      m_bUseThreshold = m_Parameters.getParameterValueAsBoolean(USETHRESHOLD);
      m_dThreshold = Math.abs(m_Parameters.getParameterValueAsDouble(THRESHOLD));

      m_SlopeLength = getNewRasterLayer(SLOPELENGTH, Sextante.getText("Slope_length"), IRasterLayer.RASTER_DATA_TYPE_FLOAT);

      m_SlopeLength.assign(0.0);

      final AnalysisExtent extent = m_SlopeLength.getWindowGridExtent();

      m_Slope = getTempRasterLayer(IRasterLayer.RASTER_DATA_TYPE_FLOAT, extent);

      m_DEM.setWindowExtent(extent);

      m_iNX = m_DEM.getNX();
      m_iNY = m_DEM.getNY();

      if (m_bUseThreshold) {
         createSlopeLayer();
      }

      final GridCell[] cells = getSortedArrayOfCells(m_DEM);
      final int iCells = cells.length;

      for (i = iCells - 1; (i > -1) && setProgress(iCells - i, iCells); i--) {
         setLength(cells[i].getX(), cells[i].getY());
      }

      return !m_Task.isCanceled();

   }


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("Slope_length"));
      setGroup(Sextante.getText("Indices_and_other_hydrological_parameters"));
      setUserCanDefineAnalysisExtent(true);

      try {
         m_Parameters.addInputRasterLayer(DEM, Sextante.getText("Elevation"), true);
         m_Parameters.addNumericalValue(THRESHOLD, Sextante.getText("Slope_change_threshold"), 0.5,
                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
         m_Parameters.addBoolean(USETHRESHOLD, Sextante.getText("Use_threshold"), true);
         addOutputRasterLayer(SLOPELENGTH, Sextante.getText("Slope_length"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }


   private void createSlopeLayer() {

      int x, y;

      for (y = 0; y < m_iNY; y++) {
         for (x = 0; x < m_iNX; x++) {
            m_Slope.setCellValue(x, y, m_DEM.getSlope(x, y));
         }
      }

   }


   private void setLength(final int x,
                          final int y) {

      int i, ix, iy;
      double dSlope, dSlope2;
      double dLength;
      double dValue;

      dValue = m_DEM.getCellValueAsDouble(x, y);
      if (!m_DEM.isNoDataValue(dValue) && ((i = m_DEM.getDirToNextDownslopeCell(x, y)) >= 0)) {
         ix = x + m_iOffsetX[i];
         iy = y + m_iOffsetY[i];
         dValue = m_DEM.getCellValueAsDouble(ix, iy);
         if (!m_DEM.isNoDataValue(dValue)) {
            if (m_bUseThreshold) {
               dSlope = m_Slope.getCellValueAsDouble(x, y);
               dSlope2 = m_Slope.getCellValueAsDouble(ix, iy);
               if (Math.abs(dSlope2) > m_dThreshold * Math.abs(dSlope)) {
                  dLength = m_SlopeLength.getCellValueAsDouble(x, y) + m_DEM.getDistToNeighborInDir(i);
               }
               else {
                  dLength = m_DEM.getDistToNeighborInDir(i);
               }
               if (dLength > m_SlopeLength.getCellValueAsDouble(ix, iy)) {
                  m_SlopeLength.setCellValue(ix, iy, dLength);
               }
            }
            else {
               dLength = m_SlopeLength.getCellValueAsDouble(x, y) + m_DEM.getDistToNeighborInDir(i);
               if (dLength > m_SlopeLength.getCellValueAsDouble(ix, iy)) {
                  m_SlopeLength.setCellValue(ix, iy, dLength);
               }
            }
         }
      }
      else {
         m_SlopeLength.setNoData(x, y);
      }

   }


   public GridCell[] getSortedArrayOfCells(final IRasterLayer layer) {

      int i;
      int iX, iY;
      final int iNX = layer.getNX();
      final int iCells = layer.getNX() * layer.getNY();
      GridCell[] cells;
      GridCell cell;

      cells = new GridCell[iCells];

      for (i = 0; i < iCells; i++) {
         iX = i % iNX;
         iY = i / iNX;
         cell = new GridCell(iX, iY, layer.getCellValueAsDouble(iX, iY));
         cells[i] = cell;
      }

      Arrays.sort(cells);

      return cells;

   }


}
