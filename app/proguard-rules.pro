# Activities and services declared in the manifest are kept by AAPT's generated rules, and the app
# uses no reflection of its own, so R8 needs nothing more. Keep line numbers for crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
