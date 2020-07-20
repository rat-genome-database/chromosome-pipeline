package edu.mcw.rgd.dataload;

import edu.mcw.rgd.datamodel.Chromosome;
import edu.mcw.rgd.process.FileDownloader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mtutaj
 * Date: 1/30/15
 * <p>
 * load chromosome sizes
 */
public class ChromosomeSizesLoader {
    private String version;
    private String assemblyReportFile;
    private String assemblyStatsFile;

    private ChromosomeDAO dao = new ChromosomeDAO();
    private String gcfDir;

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
            System.out.println("  SCAFFOLD-ONLY LOAD");
        } else {
            System.out.println("  CHROMOSOME-ONLY LOAD (not scaffolds)");
        }

        Map<String,Integer> scaffoldLengths = loadScaffolds ? new HashMap<>() : null;
        Map<String,ChrInfo> chrAccIds = getChromosomeAccIds(assemblyId, assemblyName, scaffoldLengths);
        System.out.println("chromosomes in assembly: "+chrAccIds.size());
        for( Map.Entry<String,ChrInfo> entry: chrAccIds.entrySet() ) {
            ChrInfo ci = entry.getValue();
            String chrAccId = ci.refseqId;
            Chromosome chr = dao.createChromosome(mapKey, entry.getKey(), chrAccId);
            chr.setGenbankId(ci.genbankId);

            if( loadScaffolds ) {
                chr.setSeqLength(scaffoldLengths.get(chrAccId));
            } else {
                // download file with chromosome
                getChromosomeStats(assemblyId, assemblyName, chr);
            }

            // parse it and read
            dao.updateChromosome(chr);
        }
        System.out.println("  OK!");
    }

    String getExternalFileNamePrefix(String assemblyId, String assemblyName) {
        return getGcfDir() + assemblyId.substring(4, 7)
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

            // in 4nd col, there must be a 'assembled-molecule'
            String seqType = cols[3];
            if( !seqType.equals("assembled-molecule") )
                continue;

            if( !cols[1].equals(chr.getChromosome()) )
                continue;

            String statName = cols[4];
            int statValue = Integer.parseInt(cols[5]);

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
    Map<String,ChrInfo> getChromosomeAccIds(String assemblyId, String assemblyName, Map<String,Integer> scaffoldLengths) throws Exception {

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
            if( chr.isEmpty() || chr.length()>2 )
                continue;

            String role = cols[1];
            String refseqId = cols[6];
            String genbankId = cols[4];

            if( scaffoldLengths!=null ) {
                if (!role.equals("unplaced-scaffold"))
                    continue;
                if (!refseqId.startsWith("NW_"))
                    continue;

                chr = refseqId;
                // remove the trailing dot and version number from full accession
                // f.e. NW_004936469.1 -> NW_004936469
                int dotPos = chr.indexOf('.');
                if( dotPos>0 ) {
                    chr = chr.substring(0, dotPos);
                }

                scaffoldLengths.put(refseqId, Integer.parseInt(cols[8]));

            } else { // load chromosomes
                if (!role.equals("assembled-molecule"))
                    continue;
                if (!refseqId.startsWith("NC_"))
                    continue;
            }

            ChrInfo ci = new ChrInfo();
            ci.refseqId = refseqId;
            ci.genbankId = genbankId;
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

    class ChrInfo {
        public String refseqId;
        public String genbankId;
    }
}
