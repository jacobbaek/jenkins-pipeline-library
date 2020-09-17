#!/usr/bin/env groovy

def call() {
  exists = fileExists('./clouds.yaml')
  if (! exists) {
    sh "cp gate/clouds.yaml ."
  }
}
