-- Cria as tabelas do sistema num banco novo (não faz nada se elas já existirem)
CREATE TABLE IF NOT EXISTS clientes (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(150) NOT NULL,
  telefone VARCHAR(40),
  endereco VARCHAR(255)
) CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS orcamentos (
  id INT AUTO_INCREMENT PRIMARY KEY,
  cliente VARCHAR(150) NOT NULL,
  servicos_resumo TEXT,
  valor_total VARCHAR(40),
  data_geracao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS galeria (
  id INT AUTO_INCREMENT PRIMARY KEY,
  titulo VARCHAR(200) NOT NULL,
  categoria VARCHAR(50) NOT NULL,
  caminho_imagem VARCHAR(255) NOT NULL,
  data_publicacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS mostruario (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(120) NOT NULL,
  fabricante VARCHAR(80) NOT NULL,
  categoria VARCHAR(80) NOT NULL,
  url VARCHAR(255) NOT NULL
) CHARACTER SET utf8mb4;
