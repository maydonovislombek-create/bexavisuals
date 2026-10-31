# BexaVisuals (Fabric 1.21.4)

## Yig'ish
1. JDK 21 o'rnating.
2. Bir marta: `gradle wrapper --gradle-version 8.12` (yoki boshqa loyihangizdan gradlew/gradle papkasini ko'chiring).
3. `./gradlew build`  (Windows: `gradlew build`)
4. Jar: `build/libs/BexaVisuals-1.0.0.jar` -> `.minecraft/mods` ga joylang.

Talab: Fabric Loader 0.16.10+, Fabric API (1.21.4), Java 21. `gradle.properties` dagi
versiyalarni https://fabricmc.net/develop bilan solishtirib ko'ring.

## Boshqaruv
- **Right Shift** — menyuni ochadi/yopadi.
- Modul qatori: **chap tugma** yoqish/o'chirish, **o'ng tugma** sozlamalarni ochish, **o'rta tugma** tugma biriktirish.
- Qidiruv maydoni barcha sahifalar bo'yicha qidiradi.
- Sarlavhadan ushlab sudrab, panelni ko'chirish mumkin.
- Config sahifasida nomli presetlarni saqlash/yuklash/o'chirish mumkin. Menyu yopilganda avtomatik saqlanadi.
- **C** — Zoom (ushlab turish).

## Modullar (25 ta)

### Visuals
- Jump Circle — 20 ta model, 7 ta rang rejimi
- Block Overlay, Crosshair, Full Bright, Hit Color, Aspect Ratio, Hit Bubble
- No Render (No Hurt Cam / No Fire Overlay / No Weather)
- Zoom, FOV Changer

### ESP
- Target ESP — 15 ta rejim
- ESP (Box / Outline Box / 2D Box + sog'liq chizig'i)
- Tracers, Nametags, Glow (chams)

### Players (jangovar effektlar)
- Hit Marker, Damage Indicator, Kill Effect, Trails

### Misc
- Watermark, Info HUD (FPS/Ping/CPS/Coords), Armor HUD, Potion HUD, Keystrokes, Theme Editor

## Muhim eslatmalar
- Kod bu muhitda **haqiqiy Minecraft/Fabric kutubxonalari bilan kompilyatsiya qilib ko'rilmagan**
  (internet va Minecraft jar fayllari bu yerda yo'q). Xato chiqsa, to'liq matnini ber — tuzataman.
- Eng ehtimolli xato joylari: `mixin/*.java` fayllaridagi metod nomlari (`getBasicProjectionMatrix`,
  `tiltViewWhenHurt`, `renderCrosshair`, `renderFireOverlay`, `renderWeather`) — bu nomlar
  Yarn mappings versiyasiga qarab biroz farq qilishi mumkin. Barcha mixin inject'lar
  `require = 0` bilan yozilgan: agar nom noto'g'ri bo'lsa, o'yin build bo'ladi va ishga tushadi,
  faqat o'sha bitta effekt sukut bilan ishlamaydi (crash bo'lmaydi).
- **Qo'shilmagan**: Time Changer, Weather Changer (force), Custom Sky/Fog Color, Item Physics,
  Totem Pop, Cape, Custom Hand View, Freelook, animatsiyalar. Bular Minecraft ichki render
  kodiga chuqurroq va versiyaga ancha sezgir mixin talab qiladi; xavfni kamaytirish uchun
  hozircha qoldirildi. Asosiy build ishga tushgach, xohlasangiz shularni ham qo'shamiz.
