package es.unex.sextante.hydrology.edgeContamination;

import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.ParametersSet;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.hydrology.accFlow.AccFlowAlgorithm;
import es.unex.sextante.outputs.Output;

public class EdgeContaminationAlgorithm
         extends
            GeoAlgorithm {

   public static final String EDGECONT = "EDGECONT";
   public static final String DEM      = "DEM";
   private int                m_iNX, m_iNY;


   @Override
   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

      int x, y;
      IRasterLayer weights;

      final IRasterLayer dem = m_Parameters.getParameterValueAsRasterLayer(DEM);
      weights = getTempRasterLayer(IRasterLayer.RASTER_DATA_TYPE_BYTE, new AnalysisExtent(dem));

      weights.assign(0.0);

      m_iNX = weights.getWindowGridExtent().getNX();
      m_iNY = weights.getWindowGridExtent().getNY();

      for (y = 0; y < m_iNY; y++) {
         weights.setCellValue(1, y, 1);
         weights.setCellValue(m_iNX - 2, y, 1);
      }
      for (x = 0; x < m_iNX; x++) {
         weights.setCellValue(x, 1, 1);
         weights.setCellValue(x, m_iNY - 2, 1);
      }

      final AccFlowAlgorithm alg = new AccFlowAlgorithm();
      final ParametersSet ps = alg.getParameters();
      ps.getParameter(AccFlowAlgorithm.WEIGHTS).setParameterValue(weights);
      ps.getParameter(AccFlowAlgorithm.DEM).setParameterValue(dem);
      ps.getParameter(AccFlowAlgorithm.METHOD).setParameterValue(new Integer(3));
      final Output flowacc = alg.getOutputObjects().getOutput(AccFlowAlgorithm.FLOWACC);
      flowacc.setOutputChannel(getOutputChannel(EDGECONT));
      if (alg.execute(this.m_Task, this.m_OutputFactory)) {
         final Output out = m_OutputObjects.getOutput(EDGECONT);
         out.setOutputObject(flowacc.getOutputObject());
      }
      else {
         return false;
      }

      return !m_Task.isCanceled();
   }


   @Override
   public void defineCharacteristics() {

      setName(Sextante.getText("Edge_contamination"));
      setGroup(Sextante.getText("Indices_and_other_hydrological_parameters"));
      setUserCanDefineAnalysisExtent(false);

      try {
         m_Parameters.addInputRasterLayer(DEM, Sextante.getText("Elevation"), true);
         addOutputRasterLayer(EDGECONT, Sextante.getText("Edge_contamination"));
      }
      catch (final RepeatedParameterNameException e) {
         Sextante.addErrorToLog(e);
      }

   }

}
