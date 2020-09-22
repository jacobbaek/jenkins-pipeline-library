#!/usr/bin/env groovy

def call(Boolean success) {
  blacklist = ['']
  blacklist.each {   
    if (env.JOB_NAME.contains(it)) {
      println("Skipping notification due to blacklist config.")
      return
    }
  }

  if (success) {
    MSG = "SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
    C = '#00FF00'
  } else {
    MSG = "FAILURE: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
    C = '#FF0000'
  }

  slackSend (
    color: C,
    message: "${MSG}"
  )
}

