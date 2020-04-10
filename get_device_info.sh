if [ $# -ne 4 ]; then
	echo "Usage: sh $0 <device> <file> <device_name (any)> <config_id (any)>"
	return
fi
echo "[$4]" > $2
echo "UserReadableName = $3" >> $2
echo "Build.FINGERPRINT = $(adb shell getprop ro.build.fingerprint)" >> $2
echo "Build.HARDWARE = $(adb shell getprop ro.hardware)" >> $2
echo "Build.RADIO = unknown" >> $2
echo "Build.BOOTLOADER = unknown" >> $2
echo "Build.BRAND = $(adb shell getprop ro.product.brand)" >> $2
echo "Build.DEVICE = $(adb shell getprop ro.product.device)" >> $2
echo "Build.VERSION.SDK_INT = $(adb shell getprop ro.build.version.sdk)" >> $2
echo "Build.MODEL = $(adb shell getprop ro.product.model)" >> $2
echo "Build.MANUFACTURER = $(adb shell getprop ro.product.manufacturer)" >> $2
echo "Build.PRODUCT = $(adb shell getprop ro.vendor.product.name)" >> $2
echo "Build.ID = $(adb shell getprop ro.build.id)" >> $2
echo "Build.VERSION.RELEASE = $(adb shell getprop ro.build.version.release)" >> $2
echo "TouchScreen = 3" >> $2
echo "Keyboard = 1" >> $2
echo "Navigation = 1" >> $2
echo "ScreenLayout = 2" >> $2
echo "HasHardKeyboard = false" >> $2
echo "HasFiveWayNavigation = false" >> $2
echo "Screen.Density = 403" >> $2
echo "Screen.Width = 1080" >> $2
echo "Screen.Height = 2160" >> $2
echo "Platforms = $(adb shell getprop ro.product.cpu.abilist)" >> $2

adb shell cmd package list libraries | cut -d ":" -f2 > aux.out
while read line
  do
    echo $line | tr '\n' ',' >> res.out
  done < aux.out
shared_lib=$(cat res.out)
shared_lib=$(echo ${shared_lib%?})
echo "SharedLibraries = $shared_lib" >> $2
rm aux.out
rm res.out

adb shell cmd package list features | cut -d ":" -f2 > aux.out
while read line
  do
    echo $line | tr '\n' ',' >> res.out
  done < aux.out
features=$(cat res.out)
features=$(echo ${features%?})
echo "Features = $features" >> $2
rm aux.out
rm res.out

echo "Locales = af,af_ZA,am,am_ET,ar,ar_EG,ar_XB,ast,az,bg,bg_BG,bn,ca,ca_ES,cs,cs_CZ,da,da_DK,de,de_DE,el,el_GR,en,en_AU,en_CA,en_GB,en_IN,en_US,en_XA,es,es_ES,es_US,et,eu,fa,fa_IR,fi,fi_FI,fil,fil_PH,fr,fr_BE,fr_CA,fr_FR,gl,gu,hi,hi_IN,hr,hr_HR,hu,hu_HU,hy,in,in_ID,is,it,it_IT,iw,iw_IL,ja,ja_JP,ka,kk,km,kn,ko,ko_KR,ky,lo,lt,lt_LT,lv,lv_LV,mk,ml,mn,mr,ms,ms_MY,my,nb,nb_NO,ne,nl,nl_NL,pl,pl_PL,pt,pt_BR,pt_PT,ro,ro_RO,ru,ru_RU,si,sk,sk_SK,sl,sl_SI,sq,sr,sr_RS,sv,sv_SE,sw,sw_TZ,ta,te,th,th_TH,tr,tr_TR,uk,uk_UA,ur,uz,vi,vi_VN,zh,zh_CN,zh_HK,zh_TW,zu,zu_ZA" >> $2
echo "Client = android-google" >> $2
echo "Roaming = mobile-notroaming" >> $2
echo "GSF.version = $(adb shell dumpsys package com.google.android.gms | grep versionCode | cut -d "=" -f2 | cut -d " " -f1 | head -1)" >> $2
echo "Vending.version =  $(adb shell dumpsys package com.android.vending | grep versionCode | head -1 | cut -d "=" -f2 | cut -d " " -f1)" >> $2
echo "Vending.versionString = $(adb shell dumpsys package com.android.vending | grep versionName | head -1 | cut -d "=" -f2)" >> $2
echo "CellOperator = 21401" >> $2
echo "SimOperator = 21401" >> $2
echo "TimeZone = Europe/Madrid" >> $2
echo "GL.Version = 196610" >> $2
echo "GL.Extensions =" >> $2
