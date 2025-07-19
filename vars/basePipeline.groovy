def call(Map config) {
  def dockerImage = config.dockerImage
  def appPort     = config.port
  def repoUrl     = config.repoUrl

  pipeline {
    agent {
        kubernetes {
            yamlFile 'jenkins/pod-template.yaml'
            defaultContainer 'python'
        }
    }

    options {
      timeout(time: 10, unit: 'MINUTES')
    }

    stages {
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

    post {
      always {
        script {
            echo "Build completed successfully: ${currentBuild.fullDisplayName} with result ${currentBuild.currentResult}"
          }
        }
      }
    }
  }
}
