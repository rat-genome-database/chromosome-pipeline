package edu.mcw.rgd.dataload;

import edu.mcw.rgd.datamodel.Chromosome;
import edu.mcw.rgd.process.FileDownloader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mtutaj
 * @since 1/30/15
 * load chromosome sizes
 */
public class ChromosomeSizesLoader {
    private String version;
    private String assemblyReportFile;
    private String assemblyStatsFile;

    private ChromosomeDAO dao = new ChromosomeDAO();
    private String gcfDir;
    private String gcaDir;

    public void run(int mapKey, boolean loadScaffolds) throws Exception {
        System.out.println(getVersion());
        System.out.println("  MAP_KEY="+mapKey);

        String assemblyId = dao.getRefSeqAssemblyId(mapKey);
        if( assemblyId==null ) {
            System.out.println("ERROR: no Assembly ID for mapKey="+mapKey);
        }
        System.out.println("  ASSEMBLY_ID="+assemblyId);

        String assemblyName = dao.getRefSeqAssemblyName(mapKey);
        if( assemblyName==null ) {
            System.out.println("ERROR: no Assembly Name for mapKey="+mapKey);
        }
        System.out.println("  ASSEMBLY_NAME="+assemblyName);

        if( loadScaffolds ) {
            System.out.println("  LOAD_SCAFFOLDS=true    --- assembly scaffolds are loaded in addition to chromosomes");
        }

        Map<String,ChrInfo> chrAccIds = getChromosomeAccIds(assemblyId, assemblyName);
        int chrCount = 0;
        int scaffoldCount = 0;
        for( Map.Entry<String,ChrInfo> entry: chrAccIds.entrySet() ) {
            String chr = entry.getKey();
            ChrInfo ci = entry.getValue();
            String chrAccId = ci.refseqId;
            Chromosome c = dao.createChromosome(mapKey, chr, chrAccId);
            c.setGenbankId(ci.genbankId);

            if( chr.startsWith("NW_") ) {
                if( loadScaffolds ) {
                    scaffoldCount++;
                    c.setSeqLength(ci.seqLength);
                }
            } else {
                chrCount++;
                // download file with chromosome
                getChromosomeStats(assemblyId, assemblyName, c);
            }

            // parse it and read
            dao.updateChromosome(c);
        }
        if( chrCount!=0 ) {
            System.out.println("chromosomes in assembly: " + chrCount);
        }
        if( scaffoldCount!=0 ) {
            System.out.println("scaffolds in assembly: " + scaffoldCount);
        }
        System.out.println("  OK!");
    }

    String getExternalFileNamePrefix(String assemblyId, String assemblyName) {

        String out = assemblyId.startsWith("GCF") ? getGcfDir() : getGcaDir();
        return out + assemblyId.substring(4, 7)
                + "/" + assemblyId.substring(7, 10)
                + "/" + assemblyId.substring(10, 13)
                + "/" + assemblyId + "_" + assemblyName
                + "/" + assemblyId + "_" + assemblyName;
    }

    /**
     * get map of chromosome names mapped to chromosome accession ids
     * @return
     */
    void getChromosomeStats(String assemblyId, String assemblyName, Chromosome chr) throws Exception {

        String fileNamePrefix = getExternalFileNamePrefix(assemblyId, assemblyName);
        String path = fileNamePrefix + getAssemblyStatsFile();
        int gapCount = 0;

        // sample content of the file
        //# unit-name	molecule-name	molecule-type/loc	sequence-type	statistic	value
        //Primary Assembly	1	Chromosome	assembled-molecule	total-length	282763074
        //Primary Assembly	1	Chromosome	assembled-molecule	ungapped-length	268062321
        //Primary Assembly	1	Chromosome	assembled-molecule	scaffold-count	58
        //Primary Assembly	1	Chromosome	assembled-molecule	spanned-gaps	7723
        //Primary Assembly	1	Chromosome	assembled-molecule	unspanned-gaps	57

        FileDownloader downloader = new FileDownloader();
        downloader.setExternalFile(path);
        downloader.setLocalFile("data/" + assemblyName + ".stats.txt");
        downloader.setPrependDateStamp(true);
        String localFile = downloader.downloadNew();
        BufferedReader reader = new BufferedReader(new FileReader(localFile));
        String line;
        while( (line=reader.readLine())!=null ) {
            // skip comments and empty lines
            if( line.startsWith("#") || line.isEmpty() )
                continue;
            String[] cols = line.split("[\\t]", -1);

            // in 4th col, there must be a 'assembled-molecule'
            String seqType = cols[3];
            if( !seqType.equals("assembled-molecule") )
                continue;

            if( !cols[1].equals(chr.getChromosome()) )
                continue;

            String statName = cols[4];
            int statValue = -1;
            try {
                statValue = Integer.parseInt(cols[5]);
            } catch( NumberFormatException e ) {
                // not an integer -- try double and round it to int
                double dVal = Double.parseDouble(cols[5]);
                statValue = (int) Math.round(dVal);
            }

            if( statName.equals("total-length") )
                chr.setSeqLength(statValue);
            else if( statName.equals("ungapped-length") )
                chr.setGapLength(chr.getSeqLength()-statValue);
            else if( statName.equals("scaffold-count") )
                chr.setContigCount(statValue);
            else if( statName.equals("spanned-gaps") )
                gapCount += statValue;
            else if( statName.equals("unspanned-gaps") )
                gapCount += statValue;

        }
        reader.close();
        chr.setGapCount(gapCount);

        System.out.println(" chr"+chr.getChromosome()+", seqLen="+chr.getSeqLength()+", contigs="+chr.getContigCount()+", gaps="+chr.getGapCount());
    }

    /**
     * get map of chromosome names mapped to chromosome accession ids
     * @return
     */
    Map<String,ChrInfo> getChromosomeAccIds(String assemblyId, String assemblyName) throws Exception {

        String fileNamePrefix = getExternalFileNamePrefix(assemblyId, assemblyName);
        String path = fileNamePrefix + getAssemblyReportFile();
        System.out.println("downloading file "+path);
        Map<String,ChrInfo> chrAccIds = new HashMap<>();

        // sample content of the file
        //# Sequence-Name	Sequence-Role	Assigned-Molecule	Assigned-Molecule-Location/Type	GenBank-Accn	Relationship	RefSeq-Accn	Assembly-Unit
        //1	assembled-molecule	1	Chromosome	na	<>	NC_005100.2	Primary Assembly
        //2	assembled-molecule	2	Chromosome	na	<>	NC_005101.2	Primary Assembly
        FileDownloader downloader = new FileDownloader();
        downloader.setExternalFile(path);
        downloader.setLocalFile("data/" + assemblyName + ".assembly.txt");
        downloader.setPrependDateStamp(true);
        String localFile = downloader.downloadNew();
        BufferedReader reader = new BufferedReader(new FileReader(localFile));
        String line;
        while( (line=reader.readLine())!=null ) {
            // skip comments and empty lines
            if( line.startsWith("#") || line.isEmpty() )
                continue;
            // in 1st col, there must be a chromosome name (1-2 characters)
            // in 2nd col, there must be a 'assembled-molecule'
            // RefSeq-Accn col7, RefSeq acc id must start with 'NC_'
            String[] cols = line.split("[\\t]", -1);

            String chr = cols[2];
            if( chr.isEmpty() )
                continue;

            String role = cols[1];
            String refseqId = cols[6];
            String genbankId = cols[4];
            int length = 0;
            if( cols.length>=9 ) {
                if( !cols[8].equals("na") ) {
                    length = Integer.parseInt(cols[8]);
                }
            }

            // load chromosomes
            if( role.equals("assembled-molecule") ) {
                if( assemblyId.startsWith("GCF") ) {
                    // RefSeq assembly
                    if (!refseqId.startsWith("NC_"))
                        continue;
                } else if( assemblyId.startsWith("GCA") ) {
                    // no special handling
                }
            } else if( role.equals("unplaced-scaffold") ) {
                if (!refseqId.startsWith("NW_"))
                    continue;

                chr = refseqId;
                // remove the trailing dot and version number from full accession
                // f.e. NW_004936469.1 -> NW_004936469
                int dotPos = chr.indexOf('.');
                if( dotPos>0 ) {
                    chr = chr.substring(0, dotPos);
                }
            }

            if( refseqId!=null && refseqId.equals("na") ) {
                refseqId = null;
            }
            if( genbankId!=null && genbankId.equals("na") ) {
                genbankId = null;
            }

            ChrInfo ci = new ChrInfo();
            ci.refseqId = refseqId;
            ci.genbankId = genbankId;
            ci.seqLength = length;
            chrAccIds.put(chr, ci);
        }
        reader.close();
        return chrAccIds;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setAssemblyReportFile(String assemblyReportFile) {
        this.assemblyReportFile = assemblyReportFile;
    }

    public String getAssemblyReportFile() {
        return assemblyReportFile;
    }

    public void setAssemblyStatsFile(String assemblyStatsFile) {
        this.assemblyStatsFile = assemblyStatsFile;
    }

    public String getAssemblyStatsFile() {
        return assemblyStatsFile;
    }

    public void setGcfDir(String gcfDir) {
        this.gcfDir = gcfDir;
    }

    public String getGcfDir() {
        return gcfDir;
    }

    public String getGcaDir() {
        return gcaDir;
    }

    public void setGcaDir(String gcaDir) {
        this.gcaDir = gcaDir;
    }

    class ChrInfo {
        public String refseqId;
        public String genbankId;
        public int seqLength;
    }
}
