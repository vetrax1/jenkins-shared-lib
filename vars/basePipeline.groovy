def call(Map config) {
  def dockerImage     = config.dockerImage
  def appPort         = config.port
  def repoUrl         = config.repoUrl
  def dockerfilePath  = config.dockerfile ?: './Dockerfile'
  def contextDir      = config.contextDir ?: '.'

  pipeline {
    agent {
      kubernetes {
        yamlFile 'jenkins/kaniko-pod.yaml'
        defaultContainer 'python'
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
          container('python') {
            sh 'pip install -r requirements.txt'
          }
        }
      }

      stage('Run App Test') {
        steps {
          container('python') {
            sh 'python app.py & sleep 5 && curl http://localhost:$APP_PORT || true'
          }
        }
      }

      stage('Build and Push Image (Kaniko)') {
        steps {
          container('kaniko') {
            sh '''
              /kaniko/executor \
                --context=$CONTEXT_DIR \
                --dockerfile=$DOCKERFILE \
                --destination=$IMAGE_NAME \
                --verbosity=info
            '''
          }
        }
      }

      stage('Cleanup') {
        steps {
          container('python') {
            sh 'pkill python || true'
          }
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
