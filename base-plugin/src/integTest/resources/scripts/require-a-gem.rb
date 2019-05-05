require 'a'

if spec = Gem.loaded_specs['a']
  puts "loaded 'a' gem with version #{spec.version}"
end
