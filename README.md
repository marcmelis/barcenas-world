# barcenas_world
Propositional Logic Agents with the JADE platform and SAT4J

## Requirements

* **SAT4j 2.3.5**: https://forge.ow2.org/project/showfiles.php?group_id=228

* **Jade 4.5.0**: http://jade.tilab.com/download/jade/license/jade-download/?x=20&y=14

## Usage

```
java jade.Boot -agents 'BarcenasWorld:BarcenasWorldEnv(<worldDimension>,<barcenas_x>,<barcenas_y>);Finder:BarcenasFinder(BarcenasWorld,<worldDimension>,<stepsfile.txt>)'
```

Where:

* **worldDimension**: lenght of one side of the square map
* **barcenas_x**, **barcenas_y**: position of Barcenas
* **stepsfile.txt**: route of the steps file