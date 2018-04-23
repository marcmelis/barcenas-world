#!/bin/bash
java jade.Boot -agents 'BarcenasWorld:BarcenasWorldEnv(5,3,3,2,2);Finder:BarcenasFinder(BarcenasWorld,5,steps.txt)'
