# chromosome-pipeline
Load chromosome and cytoband information into RGD.

Chromosome and scaffold sizes are pulled from NCBI assembly reports
(`*_assembly_report.txt` and `*_assembly_stats.txt` under
`https://ftp.ncbi.nlm.nih.gov/genomes/all/GCF/` or `.../GCA/`).
Cytoband information is pulled from UCSC `cytoBandIdeo.txt.gz` files.

Chromosome information is stored in the CHROMOSOMES table.
Cytoband information is stored in the CYTO_BANDS table.

For scaffold assemblies (chinchilla, squirrel), the following pieces of information are stored:

 - map_key    (f.e. 720)
 - chromosome (f.e. NW_004939164) as used in RGD
 - refseq_id  (f.e. NW_004939164.1)
 - genbank_id (f.e. JH395895.1)
 - seq_length (f.e. 8781) — sequence length in base pairs

### SCRIPTS

`loadChromosomes.sh <map_key>` — load assembled chromosomes for the given assembly.
Only information for assembled chromosomes is loaded.

`loadScaffolds.sh <map_key>` — load both assembled chromosomes and unplaced
scaffolds. Use this for scaffold-only or mixed assemblies.

`loadCytobands.sh <map_key>` — load cytoband regions from UCSC for the given
assembly. The list of supported assemblies and their UCSC source URLs is
maintained in `properties/AppConfigure.xml` under the `cytomapLoader` bean.
