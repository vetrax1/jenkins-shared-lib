def call(Map config) {
  def dockerImage = config.dockerImage
  def appPort     = config.port
  def repoUrl     = config.repoUrl

  pipeline {
    agent any

    stages {
      stage('Checkout') {
        steps {
          git url: repoUrl
        }
      }

      stage('Install Dependencies') {
        steps {
          sh 'pip install -r requirements.txt'
        }
      }

      stage('Run App Test') {
        steps {
          sh "python app.py & sleep 5 && curl http://localhost:${appPort} || true"
        }
      }

      stage('Build Docker Image') {
        steps {
          script {
            docker.build(dockerImage)
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
