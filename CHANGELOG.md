# 1.0.7

* Fixed F key pickup not being protected in claimed chunks
* Fixed world map not updating after claiming/unclaiming chunks
* Fixed admin override not persisting across server restarts
* Added Creative mode bypass option for admin override
* Fixed thread safety issues with concurrent map access
* Fixed ChunkInfo codec parameter naming inconsistency
* Performance: Optimized TitleTickingSystem to reduce allocations
* Performance: ClaimManager now uses O(1) lookups for party/claim operations
* Performance: PartyInfo now uses O(1) lookups for member/override checks
* Changed the category name for the chunk config to make it more clear what it does

# 1.0.6

* Reworked how files are loaded and saved to be more reliable, old files should be compatible with the new system

# 1.0.5

* Added /sc admin-chunk to open the chunk gui to claim chunks using the selected admin party