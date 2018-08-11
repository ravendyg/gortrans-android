# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/me/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-dontwarn org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck
-dontnote org.osmdroid.tileprovider.MapTileProviderBase
-dontnote org.osmdroid.tileprovider.MapTileProviderBase
-dontnote org.osmdroid.tileprovider.MapTileProviderBase
-dontnote org.osmdroid.views.overlay.OverlayManager
-dontnote org.osmdroid.views.Projection
-dontnote org.osmdroid.api.IGeoPoint
-dontnote org.osmdroid.tileprovider.tilesource.ITileSource
-dontnote org.osmdroid.util.BoundingBoxE6
-dontnote org.osmdroid.util.BoundingBox
-dontnote org.osmdroid.events.MapListener
-dontnote org.osmdroid.tileprovider.MapTileProviderBase
-dontnote org.osmdroid.views.overlay.ItemizedOverlayControlView$ItemizedOverlayControlViewListener

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
