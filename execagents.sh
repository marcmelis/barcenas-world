#!/bin/bash
    java jade.Boot -gui  -agents 'BarcenasWorld:BarcenasWorldEnv(5,4,3,1,3);FINDER:BarcenasFinder(BarcenasWorld,5, steps.txt)'
