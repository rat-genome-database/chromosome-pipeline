package edu.mcw.rgd.dataload;

import edu.mcw.rgd.dao.impl.MapDAO;
import edu.mcw.rgd.dao.impl.XdbIdDAO;
import edu.mcw.rgd.datamodel.Chromosome;
import edu.mcw.rgd.datamodel.CytoBand;
import edu.mcw.rgd.datamodel.Map;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.process.Utils;

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

        // new code, as of Oct 2020
        String refSeqAssemblyAcc = map.getRefSeqAssemblyAcc();
        if( !Utils.isStringEmpty(refSeqAssemblyAcc) ) {
            return refSeqAssemblyAcc;
        }

        // old code -- for compatibility
        List<XdbId> list = xdbIdDAO.getXdbIdsByRgdId(XDB_KEY_REFSEQ_ASSEMBLY, map.getRgdId());
        if( list.size()!=1 )
            return null;
        return list.get(0).getAccId();
    }

    /**
     * given mapKey, return the assembly accession id used for fetching NCBI
     * assembly_report.txt. Returns RefSeq Assembly Acc (GCF_xxx) when set;
     * otherwise falls back to GenBank Assembly Acc (GCA_xxx). May return
     * null if the assembly has neither.
     * @param mapKey map key
     * @return GCF_xxx, or GCA_xxx, or null
     * @throws Exception
     */
    public String getAssemblyAccId(int mapKey) throws Exception {

        String acc = getRefSeqAssemblyId(mapKey);
        if( !Utils.isStringEmpty(acc) ) {
            return acc;
        }

        Map map = mapDAO.getMap(mapKey);
        return map==null ? null : map.getGenBankAssemblyAcc();
    }

    /**
     * given mapKey, return the assembly name used for constructing NCBI
     * directory paths. Returns RefSeq Assembly Name when set; otherwise
     * falls back to MAP_NAME (the natural name used by NCBI's GCA layout
     * for GenBank-only assemblies).
     * @param mapKey map key
     * @return assembly name, or null
     * @throws Exception
     */
    public String getAssemblyName(int mapKey) throws Exception {

        String name = getRefSeqAssemblyName(mapKey);
        if( !Utils.isStringEmpty(name) ) {
            return name;
        }

        Map map = mapDAO.getMap(mapKey);
        return map==null ? null : map.getName();
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

        // new code, as of Oct 2020
        String refSeqAssemblyName = map.getRefSeqAssemblyName();
        if( !Utils.isStringEmpty(refSeqAssemblyName) ) {
            return refSeqAssemblyName;
        }

        // old code -- for compatibility
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


    public int deleteChromosomesNotInSet(int mapKey, java.util.Set<String> validChromosomes) throws Exception {
        List<Chromosome> allChromosomes = mapDAO.getChromosomes(mapKey);
        int deleted = 0;
        for( Chromosome c: allChromosomes ) {
            if( !validChromosomes.contains(c.getChromosome()) ) {
                String sql = "DELETE FROM chromosomes WHERE map_key=? AND chromosome=?";
                mapDAO.update(sql, mapKey, c.getChromosome());
                deleted++;
            }
        }
        return deleted;
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
