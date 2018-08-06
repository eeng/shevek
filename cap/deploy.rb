lock "3.9.0"

set :application, "shevek"
set :deploy_to, "/var/apps/#{fetch :application}"
set :repo_url, "ssh://dev@git.vitolen.com:14000/git/#{fetch :application}.git"
set :pty, true

namespace :deploy do
  desc "Generates the jar file"
  task :package do
    on roles(:all) do |host|
      execute "cd #{release_path} && lein package && ([ -d dist ] || mkdir dist) && cp target/uberjar/shevek.jar dist"
    end
  end
  after :updated, :package

  desc "Upload Shevek config file"
  task :upload_config do
    on roles(:all) do |host|
      upload! "cap/files/config.edn", "#{release_path}/dist/config.edn"
    end
  end
  after :package, :upload_config

  after :published, 'service:shevek:restart'
end
