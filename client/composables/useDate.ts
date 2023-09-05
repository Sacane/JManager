export default function useDate() {
    function translate(month: string): string{
        let result = ''
        switch(month){
          case 'JANUARY': result = 'JANVIER'; break;
          case 'FEBRUARY': result = 'FEVRIER'; break;
          case 'MARCH': result = 'MARS'; break;
          case 'APRIL': result = 'AVRIL'; break;
          case 'MAY': result = 'MAI'; break;
          case 'JUNE': result = 'JUIN'; break;
          case 'JULY': result = 'JUILLET'; break;
          case 'AUGUST': result = 'AOUT'; break;
          case 'SEPTEMBER': result = 'SEPTEMBRE'; break;
          case 'OCTOBER': result = 'OCTOBRE'; break;
          case 'NOVEMBER': result = 'NOVEMBRE'; break;
          case 'DECEMBER': result = 'DECEMBRE'; break;
        }
        return result;
      }
      
      function monthFromNumber(num: number): string | undefined {
        switch(num) {
          case 1: return 'JANUARY'
          case 2: return 'FEBRUARY'
          case 3: return 'MARCH'
          case 4: return 'APRIL'
          case 5: return 'MAY'
          case 6: return 'JUNE'
          case 7: return 'JULY'
          case 8: return 'AUGUST'
          case 9: return 'SEPTEMBER'
          case 10: return 'OCTOBER'
          case 11: return 'NOVEMBER'
          case 12: return 'DECEMBER'
          default: return undefined
        }
      }

      const months = [
        'JANUARY', 
        'FEBRUARY',
        'MARCH',
        'APRIL',
        'MAY',
        'JUNE',
        'JULY',
        'AUGUST',
        'SEPTEMBER',
        'OCTOBER',
        'NOVEMBER',
        'DECEMBER'
      ]

      return {months, translate, monthFromNumber}
}