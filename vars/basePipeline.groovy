def call(Map config) {
  pipeline {
    agent any

    environment {
      DOCKER_IMAGE = config.dockerImage
      APP_PORT     = config.port
    }

    stages {
      stage('Checkout') {
        steps {
          git url: config.repoUrl
        }
      }

      stage('Install Dependencies') {
        steps {
          sh 'pip install -r requirements.txt'
        }
      }

      stage('Run App Test') {
        steps {
          sh "python app.py & sleep 5 && curl http://localhost:${APP_PORT} || true"
        }
      }

      stage('Build Docker Image') {
        steps {
          script {
            docker.build("${DOCKER_IMAGE}")
          }
        }
      }

      stage('Cleanup') {
        steps {
          sh 'pkill python || true'
        }
      }
    }
  }
}
