describe 'Simple' do
  it 'should' do
    expect(true).to eq true 
  end

  it 'has no $CLASSPATH entries' do
    expect($CLASSPATH.size).to eq 0
  end

  it 'has some loaded gems' do
    expect(Gem.loaded_specs.size).to eq 6
  end
end
