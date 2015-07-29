require 'credit_card_validator'

CreditCardValidator::Validator.options[:test_numbers_are_valid] = true

if !CreditCardValidator::Validator.valid?('1111 2222 3333 4444')
  puts 'Not valid'
end