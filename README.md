# chromosome-pipeline
Load chromosome and cytoband information.

Chromosome information is stored in CHROMOSOMES table.

For scaffold assemblies (chinchilla, squirrel), the following pieces of information are stored:

 - map_key    (f.e. 720)
 - chromosome (f.e. NW_004939164) as used in RGD
 - refseq_id  (f.e. NW_004939164.1)
 - genbank_id (f.e. JH395895.1)
 - seq_length (f.e. 8781)   contig length in bytes
 
 ### SCRIPTS
 For chromosome assemblies use 'loadChromosomes.sh' script. Only information for assembled chromosomes will be loaded.
 
 For scaffold only and mixed assemblies use 'loadScaffolds.sh' script: in addition to assembled chromosomes,
 the script will also load information for unplaced scaffolds.
