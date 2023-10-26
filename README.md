sudo systemctl enable docker

docker build -t elektrihind-app .

docker run -d -p 8080:8080 --restart always --name elektrihind-app-container elektrihind-app

docker run -p 8080:8080 elektrihind-app
