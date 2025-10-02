pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh './mvnw -B -DskipTests clean package'
            }
        }
    }
}

