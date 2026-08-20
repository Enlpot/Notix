# v7.45

## New Features
- Added an optional "Extract remote views text" switch (default off): when enabled, notifications without visible text will have their action button labels and content descriptions extracted and used for rule matching and history records

## Bug Fixes
- Fixed lag when swiping between "Filtered" and "By App" tabs on the History screen: group-by/sort computation is now cached and no longer fully recomputed on every recomposition
- Fixed app icons appearing with delay after tab switches or list recycling: added an in-process memory cache for app icons so PackageManager is not hit repeatedly
