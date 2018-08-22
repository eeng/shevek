Project.configure do |project|
  project.email_notifier.emails = %w(emmanicolau@gmail.com)
  project.build_command = 'DISPLAY=:1 lein ci'
end
