describe 'More' do

  it 'has no $CLASSPATH entries' do
    expect($CLASSPATH.size).to eq 5
  end

  it 'has some loaded gems' do
    require 'leafy-metrics'
    expect(Gem.loaded_specs.size).to eq 7
  end
end
