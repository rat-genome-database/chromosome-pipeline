package edu.mcw.rgd.dataload;

import edu.mcw.rgd.dao.impl.MapDAO;
import edu.mcw.rgd.dao.impl.XdbIdDAO;
import edu.mcw.rgd.datamodel.Chromosome;
import edu.mcw.rgd.datamodel.CytoBand;
import edu.mcw.rgd.datamodel.Map;
import edu.mcw.rgd.datamodel.XdbId;

import java.util.List;

/**
 * @author mtutaj
 * <p>
 * wrapper for all DAO code
 */
public class ChromosomeDAO {

    public static int XDB_KEY_REFSEQ_ASSEMBLY = 57;

    private MapDAO mapDAO = new MapDAO();
    private XdbIdDAO xdbIdDAO = new XdbIdDAO();

    /**
     * given mapKey, return RefSeq Assembly ID, f.e. 'GCF_000002285.3'
     * @param mapKey map key
     * @return RefSeq Assembly ID, or null, if not available
     * @throws Exception
     */
    public String getRefSeqAssemblyId(int mapKey) throws Exception {

        // get rgd id for the map
        Map map = mapDAO.getMap(mapKey);
        if( map==null )
            return null;

        List<XdbId> list = xdbIdDAO.getXdbIdsByRgdId(XDB_KEY_REFSEQ_ASSEMBLY, map.getRgdId());
        if( list.size()!=1 )
            return null;
        return list.get(0).getAccId();
    }

    /**
     * given mapKey, return RefSeq Assembly Name, f.e. 'CanFam3.1;
     * @param mapKey map key
     * @return RefSeq Assembly name, or null, if not available
     * @throws Exception
     */
    public String getRefSeqAssemblyName(int mapKey) throws Exception {

        // get rgd id for the map
        Map map = mapDAO.getMap(mapKey);
        if( map==null )
            return null;

        List<XdbId> list = xdbIdDAO.getXdbIdsByRgdId(XDB_KEY_REFSEQ_ASSEMBLY, map.getRgdId());
        if( list.size()!=1 )
            return null;
        return list.get(0).getNotes();
    }

    /**
     * create chromosome object in database if it does not exist
     * @param mapKey map key
     * @param chr chromosome
     * @param refSeqId chromosome RefSeq Acc id
     * @throws Exception
     */
    public Chromosome createChromosome(int mapKey, String chr, String refSeqId) throws Exception {

        Chromosome chromosome = mapDAO.getChromosome(mapKey, chr);
        if( chromosome==null ) {
            chromosome = new Chromosome();
            chromosome.setMapKey(mapKey);
            chromosome.setChromosome(chr);
            chromosome.setRefseqId(refSeqId);
            mapDAO.insertChromosome(chromosome);
        }
        else {
            chromosome.setRefseqId(refSeqId);
        }
        return chromosome;
    }

    public void updateChromosome(Chromosome chr) throws Exception {

        mapDAO.updateChromosome(chr);
    }


    public CytoBand createCytoBand(int mapKey, String chr, String bandName) throws Exception {

        CytoBand cytoBand = mapDAO.getCytoBand(mapKey, chr, bandName);
        if( cytoBand==null ) {
            cytoBand = new CytoBand();
            cytoBand.setMapKey(mapKey);
            cytoBand.setChromosome(chr);
            cytoBand.setBandName(bandName);
            mapDAO.insertCytoBand(cytoBand);
        }
        return cytoBand;
    }

    public void updateCytoBand(CytoBand cb) throws Exception {

        mapDAO.updateCytoBand(cb);
    }
}
