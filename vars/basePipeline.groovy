def call(Map config) {
  def dockerImage     = config.dockerImage
  def appPort         = config.port
  def repoUrl         = config.repoUrl
  def dockerfilePath  = config.dockerfile ?: './Dockerfile'
  def contextDir      = config.contextDir ?: '.'

  pipeline {
    agent {
      kubernetes {
        yamlFile 'jenkins/docker-pod.yaml'
        defaultContainer 'docker'
      }
    }

    options {
      timeout(time: 10, unit: 'MINUTES')
    }

    environment {
      APP_PORT     = "${appPort}"
      IMAGE_NAME   = "${dockerImage}"
      DOCKERFILE   = "${dockerfilePath}"
      CONTEXT_DIR  = "${contextDir}"
    }

    stages {
      stage('Install Dependencies') {
        steps {
          sh '''
            apk add --no-cache python3 py3-pip curl
            python3 -m venv venv
            . venv/bin/activate
            pip install -r requirements.txt
          '''
        }
      }

      stage('Run App Test') {
        steps {
          sh '''
            . venv/bin/activate
            python app.py &
            sleep 5
            curl http://localhost:$APP_PORT || true
          '''
        }
      }

      stage('Build and Push Docker Image') {
        steps {
          sh '''
            docker version
            docker build -t $IMAGE_NAME -f $DOCKERFILE $CONTEXT_DIR
            docker push $IMAGE_NAME
          '''
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
        echo "Build completed: ${currentBuild.fullDisplayName} → ${currentBuild.currentResult}"
      }
    }
  }
}
