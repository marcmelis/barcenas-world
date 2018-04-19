# barcenas_world
Propositional Logic Agents with the JADE platform and SAT4J

## Requirements

* **SAT4j 2.3.5**: https://forge.ow2.org/project/showfiles.php?group_id=228

* **Jade 4.5.0**: http://jade.tilab.com/download/jade/license/jade-download/?x=20&y=14

## Usage

```
$ java jade.Boot -agents 'BarcenasWorld:BarcenasWorldEnv(<worldDimension>,<barcenas_x>,<barcenas_y>,<mariano_x>,<mariano_y>);Finder:BarcenasFinder(BarcenasWorld,<worldDimension>,<stepsfile.txt>)'
```

Where:

* `worldDimension`: lenght of one side of the square map
* `barcenas_x`, `barcenas_y`: position of **Barcenas**
* `mariano_x`, `mariano_y`: position of **Mariano**
* `stepsfile.txt`: route of the steps file

**Barcenas** and **Mariano** are in a map of `worldDimension` x `worldDimension`. The goal of the problem is to find **Barcenas**. **Barcenas** smells at his position and in the adjacent ones in a cross. Our Agent moves around the map trying to find **Barcenas**. Our Agent can smell **Barcenas** if he is in **Barcenas** range of smell. Our Agent can meet with **Mariano** if he steps in the same position of the map. Mariano will tell to our Agent if **Barcenas** is at his right or at his left (If it is in the same column he will say right).

## stepsfile.set

`stepsfile.txt`is a flat text document with the steps our agent will do with the following format.
```
    x1,y1 x2,y2 ... xl,yl
```
Being `l` = `worldDimension`
Our Agent will just do the steps of the file by order. The steps don't need to be adjacent, you can jump from position to position.

## Example of usage

```
$ echo "1,1 1,2 2,2 2,3 3,3 4,3 4,4 4,5 5,5" > steps.txt
$ java jade.Boot -agents 'BarcenasWorld:BarcenasWorldEnv(6,4,4,2,2);Finder:BarcenasFinder(BarcenasWorld,6,steps.txt)'
```
