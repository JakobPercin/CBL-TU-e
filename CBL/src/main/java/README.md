Java Development Kit (JDK): Version 17 or higher.
No external libraries were used.

Make sure project structure is as the following:

```
CBL-TU-e-main/
|  CBL/
|  ├── src/
|  │   └── main/
|  │       └── java/
|  |            ├── level.txt
|  |            ├── textures/
|  |            |  ├── player.png
|  |            |  ├── wall.png
|  |            |  ├── floor.png
|  |            |  ├── enemy.png
|  |            |  ├── security.png
|  |            |  ├── professor.png
|  |            |  ├── item.png
|  |            |  ├── key.png
|  |            |  ├── door.png 
|  |            |  ├── sword.png
|  |            |  ├── shield.png
|  |            |  ├── arrow_right.png
|  |            |  ├── arrow_up.png
|  |            |  ├── arrow_left.png
|  |            |  ├── arrow_down.png
|  |            |  ├── fog.png
|  |            |  ├── menubg.png
|  |            |  └── student_struggles_map.png
|  │            └── Game.java
|  level.txt
|  textures/
|  ├── player.png
|  ├── wall.png
|  ├── floor.png
|  ├── enemy.png
|  ├── security.png
|  ├── professor.png
|  ├── item.png
|  ├── key.png
|  ├── door.png
|  ├── sword.png
|  ├── shield.png
|  ├── arrow_right.png
|  ├── arrow_up.png
|  ├── arrow_left.png
|  ├── arrow_down.png
|  ├── fog.png
|  ├── menubg.png
|  └── student_struggles_map.png

```
To run the game, you can run Game.java in ```.../CBL-TU-e-main/CBL/src/main/java/``` with a code editor or in command prompt:

```
cd {the rest of file directory}/CBL-TU-e-main/CBL/src/main/java/
java Game.java
```

Just some tips for the game:
ECTS is the health of the player. 
1 ATK item (shape of a sword) gives you 1 damage modifier, while 1 DEF item (shape of a shield) grants you -1 damage taken modifier (you still take 1 damage regardless of your DEF modifier count).
Resits heal 7 ECTS.
You kill enemies by bumping into them. Depending on the type of the enemy, you may need to bump into them multiple times.
If you see two consecutive arrows, that's where the exit is located. By following a particular arrow (not consecutive ones), you are guaranteed to find at least 1 key. 

Good luck.
https://github.com/JakobPercin/CBL-TU-e/
