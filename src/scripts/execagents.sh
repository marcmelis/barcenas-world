#!/bin/bash
java jade.Boot -agents 'BarcenasWorld:BarcenasWorldEnv(4,3,3);Finder:BarcenasFinder(BarcenasWorld,4,steps.txt)'
