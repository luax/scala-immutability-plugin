## Immutability analysis
This directory contains an analysis of the projects in the `projects` folder. Each project has a corresponding  build file that is configured to use the plug-in `.jar`s in this directory, `immutability_stats_plugin.jar` and `immutability_stats_plugin_with_assumptions.jar`.

### Results
| **Project**          | **Mutable** | **Shallow immutable** | **Deeply immutable** | **Conditionally deep** |
|:---------------------|------------:|----------------------:|---------------------:|-----------------------:|
| **scala-js**         |  28 (10,1%) |           131 (47,5%) |          117 (42,4%) |                 0 (0%) |
| **scala-fmt**        |  40 (20,5%) |            58 (29,7%) |           97 (49,7%) |                 0 (0%) |
| **akka-actor**       | 204 (18,3%) |           229 (20,5%) |          586 (52,5%) |              98 (8,8%) |
| **scala-test**       | 513 (23,4%) |           686 (31,3%) |          924 (42,1%) |              71 (3,2%) |
| **standard-library** | 891 (47,7%) |            131 (7,0%) |          553 (29,6%) |            292 (15,6%) |
| **signal-collect**   | 111 (40,2%) |            41 (14,9%) |           54 (19,6%) |             70 (25,4%) |


![image](https://cloud.githubusercontent.com/assets/1325939/26285864/6ef67a64-3e58-11e7-9e19-5a26f38e337a.png)
